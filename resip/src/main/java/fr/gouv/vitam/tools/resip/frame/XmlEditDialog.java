/**
 * Copyright French Prime minister Office/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@programmevitam.fr
 * <p>
 * This software is developed as a validation helper tool, for constructing Submission Information Packages (archives
 * sets) in the Vitam program whose purpose is to implement a digital archiving back-office system managing high
 * volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA archiveTransfer the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.tools.resip.frame;

import fr.gouv.vitam.tools.resip.utils.ResipException;
import fr.gouv.vitam.tools.resip.utils.ResipLogger;
import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.DataObject;
import fr.gouv.vitam.tools.sedalib.metadata.SEDAMetadata;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.xml.IndentXMLTool;
import fr.gouv.vitam.tools.sedalib.xml.SEDAXMLEventReader;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static fr.gouv.vitam.tools.resip.sedaobjecteditor.SEDAObjectEditor.*;
import static fr.gouv.vitam.tools.resip.sedaobjecteditor.SEDAObjectEditorConstants.translateTag;
import static fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger.getMessagesStackString;

/**
 * The Class XmlEditDialog.
 * <p>
 * Class for editing XML dialog acting on different sources AU, GOT and part of AU.
 */
public class XmlEditDialog extends JDialog {

    /**
     * The actions components.
     */
    private RSyntaxTextArea xmlTextArea;
    private JTextArea informationTextArea;
    private JPanel informationPanel;

    /**
     * The data.
     */
    private Object xmlObject;

    /**
     * The result.
     */
    private Object xmlObjectResult;

    // Dialog test context

    /**
     * The entry point of dialog test.
     *
     * @param args the input arguments
     * @throws ClassNotFoundException          the class not found exception
     * @throws UnsupportedLookAndFeelException the unsupported look and feel exception
     * @throws InstantiationException          the instantiation exception
     * @throws IllegalAccessException          the illegal access exception
     * @throws NoSuchMethodException           the no such method exception
     * @throws InvocationTargetException       the invocation target exception
     */
    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        TestDialogWindow window = new TestDialogWindow(XmlEditDialog.class);
    }

    /**
     * Instantiates a new XmlEditDialog for test.
     *
     * @param owner the owner
     * @throws SEDALibException the seda lib exception
     */
    public XmlEditDialog(JFrame owner) throws SEDALibException {
        this(owner, createSEDAMetadataSample("AgentType","Writer",false));
    }

    /**
     * Create the dialog.
     *
     * @param owner     the owner
     * @param xmlObject the xml object
     */
    public XmlEditDialog(JFrame owner, Object xmlObject) {
        super(owner, "", true);
        this.xmlObject = xmlObject;
        String title, presentationName, presentationText, xmlData = "";
        GridBagConstraints gbc;

        if (xmlObject instanceof DataObject) {
            DataObject dataObject = (DataObject) xmlObject;
            title = "Edition DataObject";
            presentationName = "DataObject :";
            presentationText = dataObject.getInDataObjectPackageId();
            try {
                xmlData = dataObject.toSedaXmlFragments();
                xmlData = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                        .indentString(xmlData);
            } catch (Exception e) {
                ResipLogger.getGlobalLogger().log(ResipLogger.STEP, "Resip.InOut: Erreur à l'indentation du DataObject ["
                        + dataObject.getInDataObjectPackageId() + "]",e);
            }

        } else if (xmlObject instanceof SEDAMetadata) {
            SEDAMetadata sm = (SEDAMetadata) xmlObject;
            title = "Edition partielle de métadonnées";
            presentationName = sm.getXmlElementName() + " :";
            presentationText = getSEDAMetadataInformation(sm);
            try {
                xmlData = sm.toString();
            } catch (Exception e) {
                ResipLogger.getGlobalLogger().log(ResipLogger.STEP, "Resip.InOut: Erreur à la génération XML de la métadonnée ["
                        + sm.getXmlElementName() + "]",e);
            }
        } else if (xmlObject instanceof ArchiveUnit) {
            ArchiveUnit au = (ArchiveUnit) xmlObject;
            title = "Edition " + translateTag("ArchiveUnit");
            presentationName = "xmlID:" + au.getInDataObjectPackageId();
            presentationText = "";
            xmlData = au.toSedaXmlFragments();
        } else if (xmlObject instanceof String) {
            title = "Edition XML";
            presentationName = translateTag("AnyXMLType");
            presentationText = "";
            xmlData = (String) xmlObject;
        } else {
            dispose();
            return;
        }

        setTitle(title);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(800, 500));

        final JPanel presentationPanel = new JPanel();
        presentationPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(presentationPanel, gbc);
        JLabel presentationLabel = new JLabel();
        presentationLabel.setText(presentationName);
        presentationLabel.setFont(MainWindow.BOLD_LABEL_FONT);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        presentationPanel.add(presentationLabel, gbc);
        JTextArea presentationTextArea = new JTextArea();
        presentationTextArea.setText(presentationText);
        presentationTextArea.setEditable(false);
        presentationTextArea.setFont(MainWindow.LABEL_FONT);
        presentationTextArea.setBackground(MainWindow.GENERAL_BACKGROUND);
        presentationTextArea.setLineWrap(true);
        presentationTextArea.setWrapStyleWord(true);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        presentationPanel.add(presentationTextArea, gbc);

        xmlTextArea = new RSyntaxTextArea(20, 120);
        xmlTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        SyntaxScheme scheme = xmlTextArea.getSyntaxScheme();
        scheme.getStyle(Token.MARKUP_TAG_DELIMITER).foreground = COMPOSITE_LABEL_MARKUP_COLOR;
        scheme.getStyle(Token.MARKUP_TAG_NAME).foreground = COMPOSITE_LABEL_COLOR;
        scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE).foreground = COMPOSITE_LABEL_MARKUP_COLOR;
        scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE_VALUE).foreground = COMPOSITE_LABEL_ATTRIBUTE_COLOR;
        xmlTextArea.setCodeFoldingEnabled(true);
        xmlTextArea.setFont(MainWindow.DETAILS_FONT);
        xmlTextArea.setText(xmlData);
        xmlTextArea.setCaretPosition(0);
        JScrollPane editScrollPane = new RTextScrollPane(xmlTextArea);
        editScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        editScrollPane.setMinimumSize(new Dimension(200, 100));
        gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPane.add(editScrollPane, gbc);

        informationPanel = new JPanel();
        informationPanel.setLayout(new GridBagLayout());
        informationPanel.setVisible(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(informationPanel, gbc);
        JLabel informationLabel = new JLabel();
        informationLabel.setText("");
        informationLabel.setIcon(new ImageIcon(getClass().getResource("/icon/dialog-warning.png")));
        informationLabel.setFont(MainWindow.BOLD_LABEL_FONT);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        informationPanel.add(informationLabel, gbc);
        informationTextArea = new JTextArea("");
        informationTextArea.setFont(MainWindow.LABEL_FONT);
        informationTextArea.setEditable(false);
        informationTextArea.setLineWrap(true);
        informationTextArea.setWrapStyleWord(true);
        informationTextArea.setBackground(MainWindow.GENERAL_BACKGROUND);
        informationTextArea.setForeground(Color.RED);
        gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 5);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        informationPanel.add(informationTextArea, gbc);

        JPanel actionPanel = new JPanel();
        GridBagLayout gbl_buttonPane = new GridBagLayout();
        actionPanel.setLayout(gbl_buttonPane);
        gbc = new GridBagConstraints();
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 3;
        getContentPane().add(actionPanel, gbc);

        final JButton indentButton = new JButton("Indenter");
        indentButton.addActionListener(arg -> buttonIndent());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.gridx = 0;
        actionPanel.add(indentButton, gbc);

        int buttonPlace = 1;
        if (xmlObject instanceof SEDAMetadata) {
            final JButton cleanButton = new JButton("Nettoyer");
            cleanButton.addActionListener(arg -> buttonClean());
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 5, 5);
            gbc.weightx = 1.0;
            gbc.gridy = 0;
            gbc.gridx = buttonPlace++;
            actionPanel.add(cleanButton, gbc);
        }

        final JButton saveButton = new JButton((xmlObject instanceof String?"Valider":"Sauver"));
        saveButton.addActionListener(arg -> buttonSaveXmlEdit());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 5);
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.gridx = buttonPlace++;
        actionPanel.add(saveButton, gbc);

        if (xmlObject instanceof ArchiveUnit) {
            final JButton canonizeButton = new JButton("Ordonner");
            canonizeButton.addActionListener(arg -> buttonCanonizeXmlEdit());
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 5, 5);
            gbc.weightx = 1.0;
            gbc.gridy = 0;
            gbc.gridx = buttonPlace++;
            actionPanel.add(canonizeButton, gbc);
        }

        final JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(arg -> buttonCancel());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.gridx = buttonPlace;
        actionPanel.add(cancelButton, gbc);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelButton.doClick();
            }
        });

        pack();
        informationPanel.setVisible(false);
        pack();
        setLocationRelativeTo(owner);
    }

    // actions

    private void showWarning(String text) {
        informationTextArea.setText(text);
        if (!informationPanel.isVisible()) {
            Dimension dim = this.getSize();
            informationPanel.setVisible(true);
            pack();
            dim.height = dim.height + informationPanel.getHeight();
            this.setSize(dim);
            this.setPreferredSize(dim);
            pack();
        }
    }

    private void hideWarning() {
        if (informationPanel.isVisible()) {
            informationPanel.setVisible(false);
            Dimension dim = this.getSize();
            dim.height = dim.height - informationPanel.getHeight();
            this.setSize(dim);
            this.setPreferredSize(dim);
            pack();
        }
    }

    /**
     * Button indent.
     */
    public void buttonIndent() {
        try {
            String xml = xmlTextArea.getText();
            String indentedString = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT).indentString(xml);
            xmlTextArea.setText(indentedString);
            hideWarning();
            xmlTextArea.setCaretPosition(0);
        } catch (Exception e) {
            showWarning(e.getMessage());
        }
    }

    /**
     * The Default values.
     */
    static ArrayList<String> defaultValues = new ArrayList<String>(Arrays.asList("Text",
            "1970-01-01", "1970-01-01T01:00:00", "Rule1", "Rule2", "Rule3", "Rule4",
            "Level1", "Owner1", "Text1", "Text2"));

    private String filterDefaultValues(SEDAXMLEventReader xmlReader) throws XMLStreamException {
        String result = "", tag, tmp, attrStr = "";
        XMLEvent mainEvent, subEvent, tmpEvent;
        mainEvent = xmlReader.peekUsefullEvent();
        if (mainEvent.isEndElement())
            return null;
        mainEvent = xmlReader.nextUsefullEvent();
        tag = mainEvent.asStartElement().getName().getLocalPart();
        Iterator<Attribute> attributes = mainEvent.asStartElement().getAttributes();
        while (attributes.hasNext()) {
            Attribute attribute = attributes.next();
            attrStr += " " + (attribute.getName().getPrefix().equals("xml") ? "xml:" : "") +
                    attribute.getName().getLocalPart() + "=\"" + attribute.getValue() + "\"";
        }
        tmpEvent = xmlReader.peekUsefullEvent();
        if (tmpEvent.isCharacters()) {
            String value = tmpEvent.asCharacters().getData();
            if (!defaultValues.contains(value)) {
                result = "<" + tag + attrStr + ">" + value + "</" + tag + ">";
            }
            tmpEvent = xmlReader.nextUsefullEvent();
            tmpEvent = xmlReader.nextUsefullEvent();
        } else {
            while ((tmp = filterDefaultValues(xmlReader)) != null) {
                if (result.isEmpty() && !tmp.isEmpty()) {
                    result = "<" + tag + ">";
                }
                result += tmp;
            }
            if (!result.isEmpty())
                result += "</" + tag + ">";
            tmpEvent = xmlReader.nextUsefullEvent();
        }
        return result;
    }

    /**
     * Button clean.
     */
    public void buttonClean() {
        String result = "";
        try {
            // indent to verify XML format
            String xmlDataString = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                    .indentString(xmlTextArea.getText());
            try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlDataString.getBytes(StandardCharsets.UTF_8));
                 SEDAXMLEventReader xmlReader = new SEDAXMLEventReader(bais, true)) {
                // jump StartDocument
                xmlReader.nextUsefullEvent();
                result = filterDefaultValues(xmlReader);
                XMLEvent event = xmlReader.xmlReader.peek();
                if (!event.isEndDocument())
                    throw new ResipException("Il y a des champs en trop");
            } catch (XMLStreamException | SEDALibException | IOException e) {
                throw new ResipException("Erreur de lecture", e);
            }
            if (!result.isEmpty())
                result = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                        .indentString(result);
            xmlTextArea.setText(result);
            hideWarning();
            xmlTextArea.setCaretPosition(0);
        } catch (Exception e) {
            showWarning(getMessagesStackString(e));
        }
    }

    private void buttonCancel() {
        setVisible(false);
    }

    private void buttonSaveXmlEdit() {
        try {
            String xmlDataString = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                    .indentString(xmlTextArea.getText());
            if (xmlObject instanceof DataObject) {
                DataObject dataObject = (DataObject) xmlObject;
                dataObject.fromSedaXmlFragments(xmlDataString);
                xmlObjectResult = dataObject;
            } else if (xmlObject instanceof SEDAMetadata) {
                SEDAMetadata sm = (SEDAMetadata) xmlObject;
                xmlObjectResult = SEDAMetadata.fromString(xmlDataString, sm.getClass());
            } else if (xmlObject instanceof ArchiveUnit) {
                ArchiveUnit au = (ArchiveUnit) xmlObject;
                au.fromSedaXmlFragments(xmlDataString);
                xmlObjectResult = au;
            }
            else if (xmlObject instanceof String) {
                xmlObjectResult = xmlDataString;
            }
            informationTextArea.setForeground(Color.BLACK);
            informationTextArea.setText("");
            setVisible(false);
        } catch (Exception e) {
            showWarning(getMessagesStackString(e));
        }
    }

    private void buttonCanonizeXmlEdit() {
        try {
            String xmlDataString = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                    .indentString(xmlTextArea.getText());
            ArchiveUnit au = new ArchiveUnit();
            au.fromSedaXmlFragments(xmlDataString);
            au.getContent();
            au.getManagement();
            au.getArchiveUnitProfile();
            String xmlData = au.toSedaXmlFragments();
            xmlData = IndentXMLTool.getInstance(IndentXMLTool.STANDARD_INDENT)
                    .indentString(xmlData);
            xmlTextArea.setText(xmlData);
            informationTextArea.setForeground(Color.BLACK);
            informationTextArea.setText("");
        } catch (Exception e) {
            showWarning(getMessagesStackString(e));
        }
    }

    /**
     * Get the dialog result xml string.
     *
     * @return the xml object
     */
    public Object getResult() {
        return xmlObjectResult;
    }

    /**
     * Get the dialog return value.
     *
     * @return the return value
     */
    public boolean getReturnValue() {
        return !(xmlObjectResult == null);
    }
}
