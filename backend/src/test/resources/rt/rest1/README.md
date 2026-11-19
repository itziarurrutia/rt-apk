# Fixtures de RT REST 1.0

Respuestas de ejemplo de la API `/REST/1.0`. Son **sintéticas**, creadas a partir del formato
documentado de RT. Sustitúyelas por respuestas reales de vuestro RT, anonimizadas, en cuanto
podáis, y vuelve a pasar los tests:

```bash
RT="https://rt.example.org/REST/1.0"
AUTH=(--data-urlencode "user=USUARIO" --data-urlencode "pass=CONTRASEÑA")
curl -s -G "${AUTH[@]}" "$RT/ticket/1234/show" > ticket-show.txt
curl -s -G "${AUTH[@]}" --data-urlencode "format=l" "$RT/ticket/1234/history" > history-long.txt
curl -s -G "${AUTH[@]}" "$RT/search/ticket" \
  --data-urlencode "query=Requestor = 'alguien@example.org'" \
  --data-urlencode "format=l" \
  --data-urlencode "fields=Subject,Queue,Status,Owner,Requestors,Priority,Created,LastUpdated" > search-long.txt
```

Puntos que conviene comprobar con datos reales:
- El formato de las fechas en `show` (depende del idioma y de la zona horaria del usuario de RT).
- El mensaje que devuelve RT al añadir una respuesta (`# Message recorded`, `# Correspondence added`...).
