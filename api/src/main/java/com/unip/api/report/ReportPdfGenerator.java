package com.unip.api.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.unip.api.report.dto.OverdueActionResponse;
import com.unip.api.report.dto.SummaryResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ReportPdfGenerator {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY);
    private static final Color HEADER_BACKGROUND = new Color(230, 230, 230);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] build(SummaryResponse summary, List<OverdueActionResponse> overdue,
                        LocalDateTime generatedAt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Environmental Incident Report", TITLE));
            Paragraph meta = new Paragraph("Generated on " + generatedAt.format(DATE_TIME), MUTED);
            meta.setSpacingAfter(12);
            document.add(meta);

            document.add(new Paragraph("Total incidents: " + summary.total(), HEADING));
            addCounts(document, "Incidents by status", "Status", summary.byStatus());
            addCounts(document, "Incidents by type", "Type", summary.byType());
            addCounts(document, "Incidents by sector", "Sector", summary.bySector());
            addOverdue(document, overdue);

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate the PDF report", e);
        }
        return out.toByteArray();
    }

    private void addCounts(Document document, String title, String column,
                           Map<String, Long> data) throws DocumentException {
        document.add(sectionTitle(title));
        if (data.isEmpty()) {
            document.add(new Paragraph("No data.", MUTED));
            return;
        }
        PdfPTable table = new PdfPTable(new float[] {3f, 1f});
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(headerCell(column));
        table.addCell(headerCell("Count"));
        data.forEach((name, count) -> {
            table.addCell(cell(name));
            table.addCell(cell(String.valueOf(count)));
        });
        document.add(table);
    }

    private void addOverdue(Document document, List<OverdueActionResponse> overdue)
            throws DocumentException {
        document.add(sectionTitle("Overdue actions"));
        if (overdue.isEmpty()) {
            document.add(new Paragraph("No overdue actions.", MUTED));
            return;
        }
        PdfPTable table = new PdfPTable(new float[] {3f, 3.5f, 2.5f, 2f, 1.5f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1); // the header repeats on every page
        for (String header : List.of("Incident", "Action", "Responsible", "Deadline", "Days overdue")) {
            table.addCell(headerCell(header));
        }
        for (OverdueActionResponse o : overdue) {
            table.addCell(cell("#" + o.incidentId() + " " + o.incidentTitle()));
            table.addCell(cell(o.description()));
            table.addCell(cell(o.responsible()));
            table.addCell(cell(o.deadline().format(DATE)));
            table.addCell(cell(String.valueOf(o.daysOverdue())));
        }
        document.add(table);
    }

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, HEADING);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD));
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(5);
        return cell;
    }
}