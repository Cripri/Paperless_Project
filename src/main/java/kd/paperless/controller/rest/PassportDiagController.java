package kd.paperless.controller.rest;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kd.paperless.service.PassportPdfServiceDiag;

@RestController
@RequestMapping("/passport/diag")
public class PassportDiagController {

    private final PassportPdfServiceDiag svc;
    public PassportDiagController(PassportPdfServiceDiag svc){ this.svc = svc; }

    @GetMapping("/ping")
    public Map<String,Object> ping() {
        return svc.pingTemplate();
    }

    @GetMapping(value="/echo.pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> echo() throws Exception {
        String id = svc.echoTemplate();
        byte[] bytes = svc.loadBytes(id);
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"echo.pdf\"")
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping(value="/black.pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> black() throws Exception {
        String id = svc.debugFillBlack();
        byte[] bytes = svc.loadBytes(id);
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"black.pdf\"")
                .contentLength(bytes.length)
                .body(bytes);
    }
}
