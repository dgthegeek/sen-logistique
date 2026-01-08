package sn.votreplateforme.logistique.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Service pour générer des PDF contenant des QR codes
 * Format : 2 colonnes x 3 lignes = 6 QR codes par page A4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QRCodePdfService {

    private final LivraisonRepository livraisonRepository;

    // Configuration des tailles
    private static final int QR_CODE_SIZE = 200; // pixels
    private static final float CELL_WIDTH = 280f; // points
    private static final float CELL_HEIGHT = 380f; // points

    /**
     * Génère un PDF avec les QR codes des livraisons
     *
     * @param livraisonIds Liste des IDs des livraisons
     * @return Bytes du PDF généré
     */
    public byte[] generateQRCodesPdf(List<Long> livraisonIds) {
        log.info("Génération du PDF pour {} livraisons", livraisonIds.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Créer le document PDF
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            // Marges
            document.setMargins(20, 20, 20, 20);

            // Titre
            addTitle(document);

            // Récupérer les livraisons
            List<Livraison> livraisons = livraisonRepository.findAllById(livraisonIds);

            if (livraisons.isEmpty()) {
                throw new BusinessException("Aucune livraison trouvée avec les IDs fournis");
            }

            // Créer la table (2 colonnes)
            Table table = new Table(2);
            table.setWidth(UnitValue.createPercentValue(100));

            // Générer les QR codes
            int count = 0;
            for (Livraison livraison : livraisons) {
                Cell cell = createQRCodeCell(livraison);
                table.addCell(cell);
                count++;
            }

            // Si nombre impair, ajouter une cellule vide
            if (count % 2 != 0) {
                table.addCell(new Cell().setBorder(null));
            }

            document.add(table);

            // Footer
            addFooter(document, livraisons.size());

            document.close();

            log.info("PDF généré avec succès : {} QR codes", livraisons.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Erreur lors de la génération du PDF", e);
            throw new BusinessException("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    /**
     * Crée une cellule contenant un QR code avec les informations de livraison
     */
    private Cell createQRCodeCell(Livraison livraison) {
        Cell cell = new Cell();
        cell.setWidth(CELL_WIDTH);
        cell.setHeight(CELL_HEIGHT);
        cell.setPadding(10);
        cell.setTextAlignment(TextAlignment.CENTER);

        try {
            // Numéro de tracking (grand et en gras)
            Paragraph tracking = new Paragraph(livraison.getNumeroTracking())
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            cell.add(tracking);

            // Générer le QR code
            BufferedImage qrImage = generateQRCodeImage(livraison.getQrCodeUrl());
            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(qrImage, "PNG", qrBaos);

            Image qrCodeImage = new Image(ImageDataFactory.create(qrBaos.toByteArray()));
            qrCodeImage.setWidth(150);
            qrCodeImage.setHeight(150);
            qrCodeImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
            qrCodeImage.setMarginBottom(10);
            cell.add(qrCodeImage);

            // Informations client
            cell.add(new Paragraph(livraison.getNomClient())
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(5));

            // Adresse
            String adresse = String.format("%s, %s",
                    livraison.getAdresseDestination().getQuartier(),
                    livraison.getAdresseDestination().getCommune());
            cell.add(new Paragraph(adresse)
                    .setFontSize(10)
                    .setMarginTop(2));

            // Montant COD
            NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
            String montant = currencyFormat.format(livraison.getMontantCOD()) + " FCFA";

            Paragraph montantPara = new Paragraph("💰 " + montant)
                    .setFontSize(12)
                    .setBold()
                    .setFontColor(new DeviceRgb(0, 128, 0))
                    .setMarginTop(5);
            cell.add(montantPara);

            // Produit
            cell.add(new Paragraph(livraison.getDescriptionProduit())
                    .setFontSize(9)
                    .setItalic()
                    .setMarginTop(3));

            // Indicateur fragile si nécessaire
            if (livraison.getFragile() != null && livraison.getFragile()) {
                Paragraph fragile = new Paragraph("⚠️ FRAGILE")
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(ColorConstants.RED)
                        .setMarginTop(5);
                cell.add(fragile);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la création de la cellule QR code", e);
            cell.add(new Paragraph("Erreur : " + livraison.getNumeroTracking())
                    .setFontColor(ColorConstants.RED));
        }

        return cell;
    }

    /**
     * Génère une image BufferedImage du QR code
     */
    private BufferedImage generateQRCodeImage(String url) throws WriterException {
        log.info("Génération QR code pour URL: {}", url);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Ajoute le titre du document
     */
    private void addTitle(Document document) {
        Paragraph title = new Paragraph("📦 Étiquettes QR Code - Livraisons")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);
    }

    /**
     * Ajoute le footer avec le nombre de QR codes
     */
    private void addFooter(Document document, int count) {
        Paragraph footer = new Paragraph(
                String.format("Généré le %s - %d colis",
                        java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                        count))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }
}