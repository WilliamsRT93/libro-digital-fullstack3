package com.colegio.academico.service;

import com.colegio.academico.dto.NotaResponse;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Constructor de reportes PDF. Devuelve el contenido binario; el llamador decide si
 * lo envia al usuario o lo persiste en S3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final NotaService notaService;
    private final S3StorageService s3Service;

    public byte[] generarReporteAlumno(Long alumnoId) {
        // Recuperacion de las notas del alumno desde la capa de servicio.
        List<NotaResponse> notas = notaService.notasDeAlumno(alumnoId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            // Encabezado del reporte.
            doc.add(new Paragraph("Reporte de Notas")
                    .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Alumno ID: " + alumnoId));

            // Tabla con las notas del alumno.
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 4}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Asignatura").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Nota").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Descripcion").setBold()));

            BigDecimal sum = BigDecimal.ZERO;
            for (NotaResponse n : notas) {
                table.addCell(new Cell().add(new Paragraph(n.asignatura())));
                table.addCell(new Cell().add(new Paragraph(n.valor().toString())));
                table.addCell(new Cell().add(new Paragraph(n.descripcion() != null ? n.descripcion() : "")));
                sum = sum.add(n.valor());
            }
            doc.add(table);

            // Calculo del promedio si existen notas.
            if (!notas.isEmpty()) {
                BigDecimal avg = sum.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
                doc.add(new Paragraph("Promedio: " + avg).setBold());
            }

            doc.close();
            byte[] bytes = out.toByteArray();
            log.info("Reporte PDF generado para alumno={} tamanio={} bytes", alumnoId, bytes.length);

            // Persistencia de una copia en almacenamiento de objetos (best-effort).
            String key = "reportes/alumno-" + alumnoId + "-" + System.currentTimeMillis() + ".pdf";
            s3Service.upload(key, bytes, "application/pdf");
            return bytes;
        } catch (Exception ex) {
            log.error("Error generando PDF para alumno={}", alumnoId, ex);
            throw new RuntimeException("Generacion de PDF fallida", ex);
        }
    }
}
