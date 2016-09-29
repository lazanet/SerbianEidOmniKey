package freelancerwatermellon.serbianeidomnikey;

import android.content.Context;
import android.os.Bundle;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

public class PdfGenerator {
    public static final String phonePath = "assets/font/times.ttf";
    public static final String phonePath_cyr = "assets/font/arialuni.ttf";
    public static final float[] columnW = {1.6f, 1f};
    Bundle mBundle;
    Context mContext;
    Font mTitleFont;
    Font mSubTitleFont;
    Font mRegNumberFont;
    Font mLabelFont;
    Font mDataFont;
    Font mValidFont;

    public PdfGenerator(Bundle mBundle, Context mContext) {
        super();

        this.mBundle = mBundle;
        this.mContext = mContext;
        //FontFactory.register(phonePath);
        //FontFactory.defaultEmbedding = true;
        //FontFactory.defaultEncoding = "Cp1250";

        FontFactory.register(phonePath_cyr);
        FontFactory.defaultEmbedding = true;
        FontFactory.defaultEncoding = BaseFont.IDENTITY_H;


        BaseFont mBaseFont = null;
        try {
            //mBaseFont = BaseFont.createFont(phonePath, "Cp1250",
            //		BaseFont.EMBEDDED);
            mBaseFont = BaseFont.createFont(phonePath_cyr, BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED);


        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mTitleFont = new Font(mBaseFont, 14, Font.NORMAL);
        mSubTitleFont = new Font(mBaseFont, 14, Font.NORMAL);
        mRegNumberFont = new Font(mBaseFont, 12, Font.NORMAL);
        mLabelFont = new Font(mBaseFont, 12, Font.NORMAL);
        mDataFont = new Font(mBaseFont, 12, Font.NORMAL);
        mValidFont = new Font(mBaseFont, 12, Font.NORMAL);

    }

    private static void addMetaData(Document document) {
        document.addTitle("");
        document.addSubject("Using iText");
        document.addKeywords("Java, PDF, iText");
        document.addAuthor("");
        document.addCreator("");
    }

    public void generatePDF(String fileName) {
        try {
            float left = 60;
            float right = 60;
            float top = 60;
            float bottom = 60;
            Document document = new Document(PageSize.A4, left, right, top, bottom);
            PdfWriter writer = PdfWriter.getInstance(document,
                    new FileOutputStream(fileName));
            writer.setPageEvent(new HeaderFooter(
                    ""));

            document.open();
            addMetaData(document);
            addContent(document);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addContent(Document document) throws DocumentException, MalformedURLException, IOException {
        // DOCUMENT TITLE
        String citacLicneKarte = mContext.getString(R.string.app_name);
        addHeaderParagraph(document, citacLicneKarte);
        // ADD USER PHOTO
        addUserPhoto(document);
        // ADD HEADER : podaci o građaninu
        addHeaderParagraph(document, "Podaci o građaninu");
        // ADD USER DATA
        addUserData(document);
        // ADD HEADER : podaci o građaninu
        addHeaderParagraph(document, "Podaci o dokumentu");
        // ADD DOCUMENT DATA
        addDocumentData(document);
        // ADD FOOTER

    }

    private void addHeaderParagraph(Document document, String header_string) throws DocumentException {
        Paragraph ph = new Paragraph(new Phrase(header_string,
                mTitleFont));
        PdfPCell cell = new PdfPCell(ph);
        cell.setPaddingTop(5f);
        cell.setPaddingLeft(10f);
        cell.setPaddingBottom(10f);
        cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP);
        cell.setBorderWidth(1f);

        PdfPTable table = new PdfPTable(1);
        table.addCell(cell);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(100f);
        document.add(table);
    }

    private void addUserPhoto(Document document) throws IOException, DocumentException {
        // Add photo
        Image image = null;
        if (mBundle.getByteArray("eid_photo").length > 0)
            image = Image.getInstance(mBundle.getByteArray("eid_photo"));
        if (image != null) {
            image.scalePercent(50);
            //document.add(image);
            PdfPCell cell = new PdfPCell(image);
            cell.setPaddingTop(15f);
            cell.setPaddingBottom(15f);
            cell.setBorder(Rectangle.NO_BORDER);

            PdfPTable table = new PdfPTable(1);
            table.addCell(cell);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.setWidthPercentage(100f);
            document.add(table);
        }
    }

    private void addUserData(Document document) throws DocumentException {
        String prezime = mContext.getString(R.string.prezime_pdf);
        String ime = mContext.getString(R.string.ime_pdf);
        String ime_roditelja = mContext.getString(R.string.ime_roditelja_pdf);
        String datum_rodjenja = mContext.getString(R.string.datum_rodjenja_pdf);
        String mesto_rodjenja = mContext.getString(R.string.mesto_rodjenja_pdf);
        String adresa = mContext.getString(R.string.adresa_pdf);
        String jmbg = mContext.getString(R.string.jmbg_pdf);
        String pol = mContext.getString(R.string.pol_pdf);

        String lk_prezime = mBundleData("surname");
        String lk_ime = mBundleData("given_name");
        String lk_ime_roditelja = mBundleData("parent_given_name");
        String lk_datum_rodjenja = mBundleData("date_of_birth");
        String lk_mesto_rodjenja_komplet = mBundleData("place_of_birth_full_pdf");
        String lk_adresa_komplet = mBundleData("place_full_pdf");
        String lk_jmbg = mBundleData("personal_number");
        String lk_pol = mBundleData("sex");

        PdfPTable table = new PdfPTable(2); // create table with 2 columns
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(100f);      // Use whole page width
        table.setWidths(new float[]{1, 3});   // column1 : column2 = 1 : 3

        addDataTableRow(table, prezime, lk_prezime);
        addDataTableRow(table, ime, lk_ime);
        addDataTableRow(table, ime_roditelja, lk_ime_roditelja);
        addDataTableRow(table, datum_rodjenja, lk_datum_rodjenja);
        addDataTableRow(table, mesto_rodjenja, lk_mesto_rodjenja_komplet);
        addDataTableRow(table, adresa, lk_adresa_komplet);
        addDataTableRow(table, jmbg, lk_jmbg);
        addDataTableRow(table, pol, lk_pol);

        document.add(table);

//        document.add(new Paragraph(prezime + " " + lk_prezime, mDataFont));
//        document.add(new Paragraph(ime + " " + lk_ime, mDataFont));
//        document.add(new Paragraph(ime_roditelja + " " + lk_ime_roditelja,
//                mDataFont));
//        document.add(new Paragraph(datum_rodjenja + " " + lk_datum_rodjenja,
//                mDataFont));
//        document.add(new Paragraph(mesto_rodjenja + " " + lk_mesto_rodjenja_komplet,
//                mDataFont));
//        document.add(new Paragraph(adresa + " " + lk_adresa_komplet, mDataFont));
//        document.add(new Paragraph(jmbg + " " + lk_jmbg, mDataFont));
//        document.add(new Paragraph(pol + " " + lk_pol, mDataFont));
    }

    private void addDataTableRow(PdfPTable table, String cell1, String cell2) {
        PdfPCell cellA = new PdfPCell(new Paragraph(cell1, mDataFont));
        cellA.setPaddingTop(5f);
        cellA.setPaddingBottom(10f);
        cellA.setPaddingLeft(10f);
        cellA.setBorder(Rectangle.NO_BORDER);

        table.addCell(cellA);
        PdfPCell cellB = new PdfPCell(new Paragraph(cell2, mDataFont));
        cellB.setPaddingTop(5f);
        cellB.setPaddingBottom(10f);
        cellA.setPaddingLeft(10f);
        cellB.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellB);
    }

    private void addDocumentData(Document document) throws DocumentException {
        String izdao = mContext.getString(R.string.izdao_pdf);
        String broj_l_karte = mContext.getString(R.string.broj_l_karte_pdf);
        String datum_izdavanja = mContext.getString(R.string.datum_izdavanja_pdf);
        String vazi_do = mContext.getString(R.string.vazi_do_pdf);

        String lk_izdao = mBundleData("issuing_authority");
        String lk_broj_l_karte = mBundleData("doc_reg_no");
        String lk_datum_izdavanja = mBundleData("issuing_date");
        String lk_vazi_do = mBundleData("expiry_date");

        PdfPTable table = new PdfPTable(2); // create table with 2 columns
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(100f);      // Use whole page width
        table.setWidths(new float[]{1, 3});   // column1 : column2 = 1 : 3

        addDataTableRow(table, izdao, lk_izdao);
        addDataTableRow(table, broj_l_karte, lk_broj_l_karte);
        addDataTableRow(table, datum_izdavanja, lk_datum_izdavanja);
        addDataTableRow(table, vazi_do, lk_vazi_do);

        document.add(table);
    }


    private String mBundleData(String key) {
        if (!mBundle.containsKey(key))
            return "";
        else {
            Object value = mBundle.get(key);
            if ((value.toString() == "null") || (value == null))
                return "";
            return mBundle.getString(key);
        }
    }

    class HeaderFooter extends PdfPageEventHelper {
        private String name = "";

        public HeaderFooter(String name) {
            super();
            this.name = name;
        }

        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            String headerContent = name;

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(
                            headerContent, mDataFont), document.leftMargin() - 1,
                    document.top() + 10, 0);

        }
    }
}
