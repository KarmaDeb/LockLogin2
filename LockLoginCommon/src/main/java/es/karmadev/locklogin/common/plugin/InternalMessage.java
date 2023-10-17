package es.karmadev.locklogin.common.plugin;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.file.util.NamedStream;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

/**
 * LockLogin internal message
 */
public class InternalMessage {

    private static Document properties;

    static {
        update();
    }

    public static void update() {
        LockLogin lockLogin = CurrentPlugin.getPlugin();
        APISource source = (APISource) lockLogin.plugin();

        try (NamedStream stream = source.findResource("internal/lang/messages.xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();

            properties = builder.parse(stream);
            properties.normalizeDocument();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    public static String PROCESSING_COMMAND(final String label) {
        Node node = properties.getElementsByTagName("processing").item(0);
        if (node instanceof Element) {
            Element element = (Element) node;
            if (element.hasAttribute("argument")) {
                String argumentName = element.getAttribute("argument");
                return node.getTextContent().replace(argumentName, label);
            }
        }

        return "§cFailed to fetch processing message";
    }

    public static String[] USAGE(final String command) {
        NodeList node = properties.getElementsByTagName("usage");
        Node item;
        NamedNodeMap attribute;

        List<String> usages = new ArrayList<>();
        for (int i = 0; i < node.getLength(); i++) {
            item = node.item(i);
            attribute = item.getAttributes();

            if (attribute != null) {
                Node commandAttribute = attribute.getNamedItem("command");
                if (commandAttribute != null && commandAttribute.getNodeValue().equals(command)) {
                    NodeList usageData = item.getChildNodes();
                    Map<Integer, String> messageMap = new HashMap<>();

                    for (int j = 0; j < usageData.getLength(); j++) {
                        Node childNode = usageData.item(j);
                        NamedNodeMap childAttributes = childNode.getAttributes();

                        if (childAttributes != null) {
                            Node lineAttribute = childAttributes.getNamedItem("line");
                            Node blankAttribute = childAttributes.getNamedItem("blank");

                            int commandLine = Integer.parseInt(lineAttribute.getNodeValue());
                            String raw = childNode.getTextContent();
                            if (blankAttribute != null && blankAttribute.getNodeValue().equals("true")) raw = "";

                            messageMap.put(commandLine, raw);
                        }
                    }

                    List<Integer> keys = new ArrayList<>(messageMap.keySet());
                    Collections.sort(keys);

                    for (int line : keys) {
                        usages.add(messageMap.get(line));
                    }
                }
            }
        }

        return usages.toArray(new String[0]);
    }

    public static String RESPONSE_SUCCESS(final String command, final String argument, final String replacement) {
        NodeList node = properties.getElementsByTagName("response");
        Node item;
        NamedNodeMap attribute;

        for (int i = 0; i < node.getLength(); i++) {
            item = node.item(i);
            attribute = item.getAttributes();

            if (attribute != null) {
                Node commandAttribute = attribute.getNamedItem("command");
                Node argumentAttribute = attribute.getNamedItem("argument");

                if (commandAttribute != null && commandAttribute.getNodeValue().equals(command) &&
                argumentAttribute != null && argumentAttribute.getNodeValue().equals(argument)) {
                    NodeList childNodes = item.getChildNodes();

                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node child = childNodes.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("success")) {
                            NamedNodeMap childAttributes = child.getAttributes();
                            Node replacementAttribute = childAttributes.getNamedItem("argument");

                            String text = child.getTextContent();
                            if (replacementAttribute != null) {
                                text = text.replace(replacementAttribute.getNodeValue(), replacement);
                            }

                            return text;
                        }
                    }
                }
            }
        }

        return "§cFailed to fetch success response";
    }

    public static String RESPONSE_FAIL(final String command, final String argument, final String replacement) {
        NodeList node = properties.getElementsByTagName("response");
        Node item;
        NamedNodeMap attribute;

        for (int i = 0; i < node.getLength(); i++) {
            item = node.item(i);
            attribute = item.getAttributes();

            if (attribute != null) {
                Node commandAttribute = attribute.getNamedItem("command");
                Node argumentAttribute = attribute.getNamedItem("argument");

                if (commandAttribute != null && commandAttribute.getNodeValue().equals(command) &&
                        argumentAttribute != null && argumentAttribute.getNodeValue().equals(argument)) {
                    NodeList childNodes = item.getChildNodes();

                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node child = childNodes.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("fail")) {
                            NamedNodeMap childAttributes = child.getAttributes();
                            Node replacementAttribute = childAttributes.getNamedItem("argument");

                            String text = child.getTextContent();
                            if (replacementAttribute != null) {
                                text = text.replace(replacementAttribute.getNodeValue(), replacement);
                            }

                            return text;
                        }
                    }
                }
            }
        }

        return "§cFailed to fetch fail response";
    }
}
