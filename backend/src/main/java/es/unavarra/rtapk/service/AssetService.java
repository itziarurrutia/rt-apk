package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetFechaP;
import es.unavarra.rtapk.domain.AssetMatch;
import es.unavarra.rtapk.domain.AssetNotFoundException;
import es.unavarra.rtapk.domain.AssetSearchField;
import es.unavarra.rtapk.domain.InvalidSearchException;
import es.unavarra.rtapk.domain.PassIz;
import es.unavarra.rtapk.rt.AssetGateway;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static es.unavarra.rtapk.domain.AssetSearchField.INVENTARIO;
import static es.unavarra.rtapk.domain.AssetSearchField.SERIE;
import static es.unavarra.rtapk.domain.AssetSearchField.USUARIO_WINDOWS;

/**
 * Búsqueda de assets con un único texto, que puede ser un número de inventario, un número de serie o
 * un usuario de Windows. Se consulta RT por cada campo posible y se juntan los resultados.
 */
@Service
public class AssetService {

    /** Caracteres admitidos en cualquiera de los tres campos; evita meter rutas u otra cosa en la URL. */
    private static final Pattern VALID_QUERY = Pattern.compile("^[\\p{L}\\p{N}._\\-]{1,64}$");
    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    private final AssetGateway rt;
    private final RtGateway tickets;

    public AssetService(AssetGateway rt, RtGateway tickets) {
        this.rt = rt;
        this.tickets = tickets;
    }

    public List<AssetMatch> search(String query) {
        String value = query == null ? "" : query.strip();
        if (!VALID_QUERY.matcher(value).matches()) {
            throw new InvalidSearchException(
                    "Introduce un número de inventario, número de serie o usuario de Windows (letras, números, '.', '_' o '-')");
        }
        List<AssetMatch> matches = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (AssetSearchField field : candidateFields(value)) {
            for (Asset asset : rt.find(field, value)) {
                if (seen.add(asset.id())) {
                    matches.add(new AssetMatch(field, asset));
                }
            }
        }
        return matches;
    }

    public Asset getByInventario(String numeroInventario) {
        return rt.find(INVENTARIO, numeroInventario).stream().findFirst()
                .orElseThrow(() -> new AssetNotFoundException(numeroInventario));
    }

    /**
     * Pass_iz y su fragmento (caracteres 10 a 18). Al ser una contraseña, solo para agentes, y cada
     * consulta queda registrada como comentario en el asset ("El usuario X ha visto fecha_p"). Si RT no
     * registra el comentario, no se devuelve la contraseña.
     */
    public PassIz getPassIz(CurrentUser user, String numeroInventario) {
        if (!user.agent()) {
            throw new AccessDeniedException("Solo los agentes pueden consultar Pass_iz");
        }
        if (!VALID_QUERY.matcher(numeroInventario).matches()) {
            throw new InvalidSearchException("Número de inventario no válido");
        }
        AssetFechaP fechaP = rt.findFechaP(numeroInventario)
                .orElseThrow(() -> new AssetNotFoundException(numeroInventario));
        Asset asset = getByInventario(numeroInventario);
        String message = "El usuario " + user.username() + " ha visto fecha_p";
        // RT rechaza un comentario sin texto ("Empty comment with no attachments submitted")
        tickets.addComment(asset.id(), message, message);
        return PassIz.of(fechaP.passIz());
    }

    /**
     * Campos en los que tiene sentido buscar el texto, en orden de prioridad. Los números de inventario
     * son numéricos y los usuarios de Windows no; el número de serie puede ser cualquier cosa.
     */
    static List<AssetSearchField> candidateFields(String value) {
        return DIGITS.matcher(value).matches() ? List.of(INVENTARIO, SERIE) : List.of(USUARIO_WINDOWS, SERIE);
    }
}
