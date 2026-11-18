# Fixtures de RT REST 1.0

Respuestas de ejemplo de la API `/REST/1.0`. Son **sintéticas**, creadas a partir del formato
documentado de RT. Sustitúyelas por respuestas reales de vuestro RT, anonimizadas, en cuanto
podáis, y vuelve a pasar los tests:

```bash
curl -s -c cookies.txt -d "user=USUARIO" --data-urlencode "pass=CONTRASEÑA" https://rt.example.org/REST/1.0/
curl -s -b cookies.txt "https://rt.example.org/REST/1.0/ticket/1234/show" > ticket-show.txt
curl -s -b cookies.txt "https://rt.example.org/REST/1.0/ticket/1234/history?format=l" > history-long.txt
curl -s -b cookies.txt -G "https://rt.example.org/REST/1.0/search/ticket" \
  --data-urlencode "query=Requestor = 'alguien@example.org'" \
  --data-urlencode "format=l" \
  --data-urlencode "fields=Subject,Queue,Status,Owner,Requestors,Priority,Created,LastUpdated" > search-long.txt
```

Puntos que conviene comprobar con datos reales:
- El formato de las fechas en `show` (depende del idioma y de la zona horaria del usuario de RT).
- El mensaje que devuelve RT al añadir una respuesta (`# Message recorded`, `# Correspondence added`...).
