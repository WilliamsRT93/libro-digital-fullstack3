package com.colegio.academico.controller;

import com.colegio.academico.dto.NotaRequest;
import com.colegio.academico.dto.NotaResponse;
import com.colegio.academico.service.NotaService;
import com.colegio.academico.service.PdfReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del dominio academico.
 * Permite la creacion de notas y la descarga de reportes PDF por alumno.
 */
@RestController
@RequestMapping("/api/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService notaService;
    private final PdfReportService pdfService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<NotaResponse> crear(@Valid @RequestBody NotaRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        // El claim "uid" identifica al docente que registra la nota.
        Long uid = jwt.getClaim("uid");
        return ResponseEntity.ok(notaService.crear(request, uid));
    }

    @GetMapping("/reporte-pdf")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','APODERADO')")
    public ResponseEntity<byte[]> reportePdf(@RequestParam Long alumnoId) {
        // Genera el PDF y lo devuelve como descarga; ademas se persiste copia en S3.
        byte[] pdf = pdfService.generarReporteAlumno(alumnoId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "reporte-alumno-" + alumnoId + ".pdf");
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
