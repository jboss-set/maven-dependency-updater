package org.jboss.set.mavendependencyupdater.core.processingstrategies;

import org.jboss.set.mavendependencyupdater.DependencyEvaluator;
import org.jboss.set.mavendependencyupdater.configuration.Configuration;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

public class EmailReportProcessingStrategy extends HtmlReportProcessingStrategy {

    final private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final private String smtpHost;
    final private String smtpPort;
    final private String fromAddress;
    final private String toAddress;
    final private String subject;

    public EmailReportProcessingStrategy(Configuration configuration, File pomFile,
            String smtpHost, String smtpPort, String fromAddress, String toAddress, String subject) {
        super(configuration, pomFile);
        this.outputStream = new PrintStream(baos);
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
    }

    @Override
    public boolean process(List<DependencyEvaluator.ComponentUpgrade> upgrades) throws Exception {
        super.process(upgrades);

        if (upgrades.size() == 0) {
            // no upgrades detected, no email to send
            return true;
        }

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", false);
        //prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", smtpHost);
        prop.put("mail.smtp.port", smtpPort);
//        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");

        Session session = Session.getInstance(prop);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(toAddress));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(baos.toString(), "text/html");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        LOG.infof("Sending the report email to %s", toAddress);
        Transport.send(message);
        return true;
    }
}
