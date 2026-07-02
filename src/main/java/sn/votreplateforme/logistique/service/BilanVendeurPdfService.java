package sn.votreplateforme.logistique.service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.dto.BilanVendeur;
import sn.votreplateforme.logistique.dto.BilanVendeurLigne;
import sn.votreplateforme.logistique.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Génère le bilan d'un vendeur en PDF professionnel avec l'identité Dioks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BilanVendeurPdfService {

    private static final Color DIOKS_BLUE = new DeviceRgb(0, 102, 204);   // #0066cc (couleur primaire de l'app)
    private static final Color DIOKS_DARK = new DeviceRgb(10, 16, 36);    // #0a1024
    private static final Color GRIS_CLAIR = new DeviceRgb(243, 246, 250);
    private static final Color GRIS_TEXTE = new DeviceRgb(107, 114, 128);
    private static final Color GRIS_TOTAL = new DeviceRgb(226, 232, 240);
    private static final Color ROUGE = new DeviceRgb(220, 38, 38);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");

    public byte[] generer(BilanVendeur bilan) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            ajouterEntete(document);
            ajouterTitreEtPeriode(document, bilan);
            ajouterInfosVendeur(document, bilan);
            ajouterTableauProduits(document, bilan);
            ajouterPiedDePage(document, bilan);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF du bilan", e);
            throw new BusinessException("Impossible de générer le PDF du bilan");
        }
    }

    // ==================== SECTIONS ====================

    /** En-tête avec le logo Dioks (badge "D" + wordmark), reproduit depuis l'app. */
    private void ajouterEntete(Document document) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 6}))
                .setWidth(UnitValue.createPercentValue(100));
        header.setMarginBottom(4);

        Cell badge = new Cell()
                .add(new Paragraph("D").setFontSize(22).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(DIOKS_BLUE)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setWidth(40).setHeight(40);
        header.addCell(badge);

        Cell wordmark = new Cell()
                .add(new Paragraph("Dioks").setFontSize(22).setBold().setFontColor(DIOKS_BLUE).setMarginBottom(0))
                .add(new Paragraph("Logistique - Senegal").setFontSize(8).setFontColor(GRIS_TEXTE).setMarginTop(0))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingLeft(8);
        header.addCell(wordmark);

        document.add(header);

        // Filet de séparation
        Table filet = new Table(1).setWidth(UnitValue.createPercentValue(100));
        filet.addCell(new Cell().setHeight(3).setBackgroundColor(DIOKS_BLUE).setBorder(Border.NO_BORDER));
        filet.setMarginBottom(12);
        document.add(filet);
    }

    private void ajouterTitreEtPeriode(Document document, BilanVendeur bilan) {
        document.add(new Paragraph("Bilan du partenaire")
                .setFontSize(18).setBold().setFontColor(DIOKS_DARK).setMarginBottom(2));

        String periode;
        if (bilan.getPeriodeDebut() != null && bilan.getPeriodeDebut().equals(bilan.getPeriodeFin())) {
            periode = "Journee du " + bilan.getPeriodeDebut().format(DATE_FMT);
        } else {
            periode = "Du " + (bilan.getPeriodeDebut() != null ? bilan.getPeriodeDebut().format(DATE_FMT) : "-")
                    + " au " + (bilan.getPeriodeFin() != null ? bilan.getPeriodeFin().format(DATE_FMT) : "-");
        }
        document.add(new Paragraph(periode)
                .setFontSize(11).setFontColor(GRIS_TEXTE).setMarginBottom(12));
    }

    private void ajouterInfosVendeur(Document document, BilanVendeur bilan) {
        var v = bilan.getVendeur();
        if (v == null) return;

        Table infos = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));
        infos.setBackgroundColor(GRIS_CLAIR);
        infos.setMarginBottom(16);

        String boutique = v.getNomBoutique() != null ? v.getNomBoutique() : "-";
        String nomComplet = ((v.getPrenom() != null ? v.getPrenom() : "") + " "
                + (v.getNom() != null ? v.getNom() : "")).trim();

        infos.addCell(infoCell("Boutique", boutique));
        infos.addCell(infoCell("Partenaire", nomComplet.isEmpty() ? "-" : nomComplet));
        infos.addCell(infoCell("Telephone", v.getTelephone() != null ? v.getTelephone() : "-"));
        infos.addCell(infoCell("Produits au catalogue",
                bilan.getNombreProduits() != null ? String.valueOf(bilan.getNombreProduits()) : "0"));

        document.add(infos);
    }

    private Cell infoCell(String label, String valeur) {
        return new Cell()
                .add(new Paragraph(label).setFontSize(8).setFontColor(GRIS_TEXTE).setMarginBottom(0))
                .add(new Paragraph(valeur).setFontSize(11).setBold().setFontColor(DIOKS_DARK).setMarginTop(0))
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
    }

    private void ajouterTableauProduits(Document document, BilanVendeur bilan) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.2f, 4f, 2.2f, 2f, 2f, 3f}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Code"));
        table.addHeaderCell(headerCell("Produit"));
        table.addHeaderCell(headerCell("Prix unit."));
        table.addHeaderCell(headerCell("Stock actuel"));
        table.addHeaderCell(headerCell("Qte vendue"));
        table.addHeaderCell(headerCell("Montant ventes"));

        if (bilan.getLignes() == null || bilan.getLignes().isEmpty()) {
            Cell vide = new Cell(1, 6)
                    .add(new Paragraph("Aucun produit dans le catalogue.").setFontColor(GRIS_TEXTE))
                    .setTextAlignment(TextAlignment.CENTER).setPadding(12);
            table.addCell(vide);
        } else {
            boolean alterne = false;
            for (BilanVendeurLigne l : bilan.getLignes()) {
                Color bg = alterne ? GRIS_CLAIR : ColorConstants.WHITE;
                alterne = !alterne;

                table.addCell(corpsCell(l.getCode() != null ? l.getCode() : "-", bg, TextAlignment.LEFT));
                table.addCell(corpsCell(l.getNom() != null ? l.getNom() : "-", bg, TextAlignment.LEFT));
                table.addCell(corpsCell(formatFcfa(l.getPrixUnitaire()), bg, TextAlignment.RIGHT));

                Cell stockCell = corpsCell(String.valueOf(l.getStockActuel() != null ? l.getStockActuel() : 0),
                        bg, TextAlignment.CENTER);
                if (Boolean.TRUE.equals(l.getEnAlerte())) {
                    stockCell.setFontColor(ROUGE).setBold();
                }
                table.addCell(stockCell);

                table.addCell(corpsCell(String.valueOf(l.getQuantiteVendue() != null ? l.getQuantiteVendue() : 0),
                        bg, TextAlignment.CENTER));
                table.addCell(corpsCell(formatFcfa(l.getMontantVentes()), bg, TextAlignment.RIGHT));
            }
        }

        // Ligne de totaux
        table.addCell(totalCell("TOTAL", TextAlignment.LEFT));
        table.addCell(totalCell("", TextAlignment.LEFT));
        table.addCell(totalCell("", TextAlignment.RIGHT));
        table.addCell(totalCell(String.valueOf(bilan.getTotalStock() != null ? bilan.getTotalStock() : 0),
                TextAlignment.CENTER));
        table.addCell(totalCell(String.valueOf(bilan.getTotalQuantiteVendue() != null ? bilan.getTotalQuantiteVendue() : 0),
                TextAlignment.CENTER));
        table.addCell(totalCell(formatFcfa(bilan.getTotalMontantVentes()), TextAlignment.RIGHT));

        document.add(table);
    }

    private Cell headerCell(String texte) {
        return new Cell()
                .add(new Paragraph(texte).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(DIOKS_BLUE)
                .setPadding(6)
                .setBorder(Border.NO_BORDER);
    }

    private Cell corpsCell(String texte, Color bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(texte).setFontSize(9).setFontColor(DIOKS_DARK))
                .setBackgroundColor(bg)
                .setTextAlignment(align)
                .setPadding(6)
                .setBorder(Border.NO_BORDER);
    }

    private Cell totalCell(String texte, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(texte).setFontSize(10).setBold().setFontColor(DIOKS_DARK))
                .setBackgroundColor(GRIS_TOTAL)
                .setTextAlignment(align)
                .setPadding(7)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(DIOKS_BLUE, 1));
    }

    private void ajouterPiedDePage(Document document, BilanVendeur bilan) {
        String genere = bilan.getGenereLe() != null ? bilan.getGenereLe().format(DATETIME_FMT) : "";
        document.add(new Paragraph("Document genere le " + genere + " - Dioks, plateforme logistique")
                .setFontSize(8).setFontColor(GRIS_TEXTE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24));
    }

    // ==================== HELPERS ====================

    private String formatFcfa(BigDecimal montant) {
        if (montant == null) return "-";
        // Séparateur de milliers = espace ASCII (le séparateur français U+202F
        // n'est pas rendu par la police standard du PDF).
        String s = String.format(Locale.US, "%,d", montant.longValue()).replace(',', ' ');
        return s + " FCFA";
    }
}
