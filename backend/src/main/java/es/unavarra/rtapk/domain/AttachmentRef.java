package es.unavarra.rtapk.domain;

/** Adjunto de un ticket según ticket/&lt;id&gt;/attachments ("17930866: (Unnamed) (text/plain / 1.4k)"). */
public record AttachmentRef(long id, String contentType) {

    public boolean isTextPlain() {
        return "text/plain".equalsIgnoreCase(contentType);
    }
}
