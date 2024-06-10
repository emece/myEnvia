package com.cyamoda.mail.SendMail;

import com.cyamoda.mail.SendMail.config.MailModuleDestinationProperties;
import com.cyamoda.mail.SendMail.config.MailModuleOriginProperties;
import com.sun.mail.imap.IMAPFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@EnableConfigurationProperties({MailModuleOriginProperties.class, MailModuleDestinationProperties.class})
public class SendMailCommandLine implements CommandLineRunner {
    final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
    @Autowired
    private MailModuleOriginProperties  origen;

    @Autowired
    private MailModuleDestinationProperties  destino;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Host: "+origen.getHost());
        System.out.println("Folder Success: "+origen.getFolderSuccess());
        //System.out.println("destino From: "+destino.getNewFrom());

        //office();
    }
    @Scheduled(
            fixedRate = 60000L
    )
    public void office() throws IOException {
        System.out.println(""+sdf.format(new Date()));
        if(origen!=null){
            String PROTOCOL = "imap", HOST = origen.getHost(), USER = origen.getUser(),
                    PASSWORD = origen.getPassword(), ENCRYPTION_TYPE = "tls", PORT = origen.getPort();

            readEmail(PROTOCOL, HOST, USER, PASSWORD, ENCRYPTION_TYPE, PORT);
        }else{
            System.out.println("no hay contexto.");
        }


    }


    private  void readEmail(String PROTOCOL, String HOST, String USER, String PASSWORD, String ENCRYPTION_TYPE, String PORT) throws FileNotFoundException, IOException {


        /*----------------------------Código Real-------------------------------*/
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", PROTOCOL);
        if(ENCRYPTION_TYPE.length() > 0 && !ENCRYPTION_TYPE.equalsIgnoreCase("none")) {
            if(PROTOCOL.equalsIgnoreCase("imap")) {
                props.put("mail.store.protocol", "imaps");
                //props.put("mail.imaps.ssl.protocols", "TLSv1.2");
                props.put("mail.imaps.host", "outlook.office365.com");
                props.put("mail.imaps.port", "993");
                props.put("mail.imaps.ssl.enable", "true");
                //props.put("mail.imaps.starttls.enable", "true");
                props.put("mail.imaps.auth", "true");
                props.put("mail.imaps.auth.mechanisms", "XOAUTH2");
                props.put("mail.imaps.user", USER);
                //props.put("mail.debug", "true");
                //props.put("mail.debug.auth", "true");
                if(ENCRYPTION_TYPE.equalsIgnoreCase("tls")) {
                    props.setProperty("mail.imaps.starttls.enable", "true");
                }
            }
            else if(PROTOCOL.equalsIgnoreCase("pop3")) {

                //props.setProperty("ssl.SocketFactory.provider", "com.cyamoda.mail.SendMail.ExchangeSSLSocketFactory");
                //props.setProperty("mail.pop3.socketFactory.class", "com.cyamoda.mail.SendMail.ExchangeSSLSocketFactory");

                props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.setProperty("mail.pop3.socketFactory.fallback", "false");
                props.setProperty("mail.pop3.ssl.enable", "true");
                //props.setProperty("mail.pop3.auth.plain.disable", "true");

                props.setProperty("mail.pop3.auth", "true");
                props.setProperty("mail.pop3.auth.mechanisms", "XOAUTH2");
                props.setProperty("mail.pop3.socketFactory.port", PORT.length() > 0 ? PORT : "995");
                if(ENCRYPTION_TYPE.equalsIgnoreCase("tls")) {
                    props.setProperty("mail.pop3.starttls.enable", "true");
                }
            }
        }
        try {
            Session session = Session.getInstance(props);
            //session.setDebug(true);

            Store store = session.getStore("imaps");
            String token = MicrosoftAuth.getAuthToken(origen.getTenantId(), origen.getClientId(), origen.getClientSecret());
            //System.out.println("token: "+token);

            store.connect(HOST, USER, token);
            //store.issueCommand("AUTH XOAUTH2 " + token, 235);

            //store.connect(HOST, USER, PASSWORD);
            //printAllFolders(store);
            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search( getSearchCriteria());

            int maxReadNumber = 10;

            int total = messages.length > maxReadNumber ? maxReadNumber : messages.length;
            println("\nTotal_Email = " + messages.length);
            IMAPFolder procesados = (IMAPFolder)store.getFolder(origen.getFolderSuccess());
            boolean ascending = false;

            for(int index = 0; index < total; ++index) {
                int message_index = ascending ? index : messages.length - index - 1;

                try {
                    Message message = messages[message_index];
                    String subject = message.getSubject();
                    String from = message.getFrom().length > 0 ? message.getFrom()[0].toString() : "";
                    new StringBuffer();
                    println("-------------------------------------------------------------------------------------------");
                    println("-------------------------------------------------------------------------------------------");
                    println("------------------" + (index + 1) + "/" + messages.length + "----------------------" + subject);
                    println("-------------------------------------------------------------------------------------------");
                    println("-------------------------------------------------------------------------------------------");
                    printAllHeaders(message);
                    String body = "";
                    if (message.getContent() instanceof String) {
                        println("\tContent---------------Type=" + message.getContentType());
                        body = message.getContent().toString();
                        body = body.length() > 100 ? body.substring(0, 100) + "..." : body;
                        println("\t\t" + toSingleLine(body));
                    } else {
                        Map output = processMultipart((Multipart)message.getContent());
                        Object[] keys = output.keySet().toArray();

                        for(int i = 0; i < keys.length; ++i) {
                            println("\t" + keys[i].toString().toUpperCase() + "-------------------------------------------");
                            if (keys[i].toString() == "attachments") {
                                List attachments = (List)output.get("attachments");

                                for(int j = 0; j < attachments.size(); ++j) {
                                    Map attachment = (Map)attachments.get(j);
                                    println("\t\tFile_Name=" + attachment.get("fileName"));
                                }
                            } else {
                                body = output.get(keys[i].toString()).toString().trim();
                                body = body.length() > 100 ? body.substring(0, 100) + "..." : body;
                                println("\t\t[[[" + toSingleLine(body) + "]]]");
                            }
                        }
                    }

                    if (total == index + 1) {
                        println("-------------------------------------------------------------------------------------------");
                        println("-------------------------------------------------------------------------------------------");
                        println("-------------------------------------------------------------------------------------------");
                    }
                    //String from = message.getFrom().length>0?message.getFrom()[0].toString():"";
                    sendMail(from, subject, message, body);
                    //cambio sugerido por Omar
                    //sendMail(destino.getNewFrom(), subject, message, body);
                    message.setFlag(Flags.Flag.SEEN, true);
                    Message[] messages1 = new Message[]{message};
                    inbox.moveMessages(messages1, procesados);
                } catch (Exception var28) {
                    var28.printStackTrace();
                }
            }

            inbox.close(false);
            store.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private  Map processMultipart(Multipart multipart) throws Exception {
        Map output = new HashMap();
        output.put("html", "");
        output.put("text", "");
        List attachments = new ArrayList();

        for(int i = 0; i < multipart.getCount(); i++) {
            Map result = processBodyPart(multipart.getBodyPart(i));
            if (result != null) {
                if (result.containsKey("type")) {
                    if (result.get("type").toString().equalsIgnoreCase("html")) {
                        output.put("html", result.get("content").toString());
                    }
                    else if (result.get("type").toString().equalsIgnoreCase("text")) {
                        output.put("text", result.get("content").toString());
                    }
                    else if (result.get("type").toString().equalsIgnoreCase("attachment")) {
                        attachments.add(result);
                    }
                }
                if (result.containsKey("html")) {
                    output.put("html", result.get("html").toString());
                }
                if (result.containsKey("text")) {
                    output.put("text", result.get("text").toString());
                }
                if (result.containsKey("attachments")) {
                    List thisAttachments = (List) result.get("attachments");
                    for (int i2 = 0; i2 < thisAttachments.size(); i2++) {
                        attachments.add(thisAttachments.get(i2));
                    }
                }
            }
        }
        output.put("attachments", attachments);

        return output;
    }

    private  Map processBodyPart(BodyPart bodyPart) throws Exception {
        if(bodyPart.isMimeType("text/html") && bodyPart.getFileName() == null) {
            Map data = new HashMap();
            data.put("type", "html");
            data.put("content", bodyPart.getContent().toString());
            return data;
        }
        else if(bodyPart.isMimeType("text/plain") && bodyPart.getFileName() == null) {
            Map data = new HashMap();
            data.put("type", "text");
            data.put("content", bodyPart.getContent().toString());
            return data;
        }
        else if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && bodyPart.getFileName() != null) {
            try {
                Map map = new HashMap();
                map.put("type", "attachment");
                map.put("fileName", bodyPart.getFileName());
                String fileType = bodyPart.getContentType();
                map.put("fileType", fileType.contains(":") ? fileType.substring(0, fileType.indexOf(";")) : fileType);
                map.put("mimeBodyPart", bodyPart);
                return map;
            }
            catch (Exception ex) {
                println("Error_Content=" + bodyPart.getContentType());
                ex.printStackTrace();
            }
        }
        else if(bodyPart.getContentType().contains("multipart")) {
            Map o = processMultipart((Multipart) bodyPart.getContent());
            return o;
        }
        return null;
    }

    private void printAllHeaders(Message message) throws Exception {
        Enumeration enumeration = message.getAllHeaders();
        while (enumeration.hasMoreElements()) {
            Header header = (Header) enumeration.nextElement();
            boolean show = !header.getName().startsWith("X-") && !header.getName().equals("Received");
            show = show && !header.getName().startsWith("Authentication-") && !header.getName().startsWith("DKIM-");
            if (show) {
                println("\t" + header.getName() + "===" + toSingleLine(header.getValue()));
            }
        }
    }

    private  String toSingleLine(String str) throws Exception {
        return str.replaceAll("\\s+", " ");
    }

    private  SearchTerm getSearchCriteria() throws Exception {
        if (true) {
            //return null;
            return new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            //return new MessageIDTerm("CAD-oc7fMFqioVurtMPnGm63mWAA51wELBaLhtm38zvthTv0+DQ@mail.gmail.com");
        }
        FlagTerm unread = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
        //FromTerm fromTerm = new FromTerm(new InternetAddress("@gmail.com"));

        Calendar cal = Calendar.getInstance();
        /* Set date to 1 day back from now */
        cal.roll(Calendar.DATE, false);
        ReceivedDateTerm latest = new ReceivedDateTerm(DateTerm.GT, cal.getTime());
        SearchTerm andTerm = new AndTerm(unread, latest);

        //SearchTerm notFromTerm = new NotTerm(new FromTerm(new InternetAddress("black_listed_domain.com")));

        /*SubjectTerm subjectTerm = new SubjectTerm("Notable");
        OrTerm orTerm = new OrTerm(fromTerm, new OrTerm(andTerm, subjectTerm));
        AndTerm andTerm1 = new AndTerm(orTerm, notFromTerm);


        SearchTerm searchTerm = new SearchTerm() {
            @Override
            public boolean match(Message message) {
                try {
                    if (message.getSubject().contains("forwarding")) {
                        return true;
                    }
                }
                catch (MessagingException ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        };

        return andTerm1;*/
        return andTerm;

    }


    public static void main(String args[]){
        //SecurityPermission("insertProvider.JavaMail-OAuth2");
        System.out.println();
    }
    private void println(Object o) {
        System.out.println(o);
    }

    public void sendMail(String from, String sub, Message msg, String body) throws IOException {
        println("Puerto:" + destino.getPort());
        println("host:" + destino.getHost());
        println("Enviando correo a SAP...");
        Properties props = new Properties();
        props.put("mail.smtp.host", destino.getHost());
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.port", destino.getPort());
        Session session = Session.getDefaultInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);

            //message.addRecipient(Message.RecipientType.TO, new InternetAddress(config.getOriginUser()));
            //message.setFrom(new InternetAddress(from));

            //message.addRecipient(Message.RecipientType.TO, new InternetAddress(config.getOriginUser()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(origen.getUser()));
            message.setFrom(new InternetAddress(from));
            //message.setFrom(new InternetAddress(destino.getNewFrom()));
              
                       
            message.setSubject(sub);
            message.setText(body);
            Transport.send(message);
            println("message sent successfully");
        } catch (MessagingException var7) {
            throw new RuntimeException(var7);
        }
    }
}
