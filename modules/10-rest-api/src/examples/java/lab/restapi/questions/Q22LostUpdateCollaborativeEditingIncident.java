package lab.restapi.questions;

import org.springframework.http.HttpStatus;

public class Q22LostUpdateCollaborativeEditingIncident {

    public static void main(String[] args) {
        // Incident: Editor A and Editor B both fetch version 1 ("ETag: \"v1\"").
        String currentEtag = "\"v1\"";

        // Editor A submits edit with If-Match: "v1". Server accepts and bumps to "v2".
        String editorAIfMatch = "\"v1\"";
        boolean editorASucceeded = currentEtag.equals(editorAIfMatch); // true
        if (editorASucceeded) {
            currentEtag = "\"v2\"";
        }

        // Editor B submits edit with old If-Match: "v1". Server detects conflict!
        String editorBIfMatch = "\"v1\"";
        boolean editorBConflict = !currentEtag.equals(editorBIfMatch); // true
        HttpStatus editorBStatus =
                editorBConflict
                        ? HttpStatus.PRECONDITION_FAILED
                        : HttpStatus.OK; // HttpStatus.PRECONDITION_FAILED

        System.out.println(
                "Editor A succeeded, version bumped to: "
                        + currentEtag); // Editor A succeeded, version bumped to: "v2"
        System.out.println(
                "Editor B received: "
                        + editorBStatus); // Editor B received: 412 PRECONDITION_FAILED
    }
}
