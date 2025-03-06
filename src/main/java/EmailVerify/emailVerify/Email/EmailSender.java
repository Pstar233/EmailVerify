package EmailVerify.emailVerify.Email;


import java.util.Properties;

import EmailVerify.emailVerify.EmailVerify;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailSender {
    private final EmailVerify plugin;
    private String username;
    private String password;
    private Properties props;

    public EmailSender(EmailVerify plugin) {
        this.plugin = plugin;
        this.reload();
    }

    //邮件发送
    public void reload() {
        this.username = this.plugin.getConfig().getString("mail.username");
        this.password = this.plugin.getConfig().getString("mail.password");
        this.props = new Properties();
        this.props.put("mail.smtp.auth", this.plugin.getConfig().getBoolean("mail.properties.mail.smtp.auth"));
        this.props.put("mail.smtp.starttls.enable", this.plugin.getConfig().getBoolean("mail.properties.mail.smtp.starttls.enable"));
        this.props.put("mail.smtp.ssl.enable", this.plugin.getConfig().getBoolean("mail.properties.mail.smtp.ssl.enable"));
        this.props.put("mail.smtp.host", this.plugin.getConfig().getString("mail.host"));
        this.props.put("mail.smtp.port", this.plugin.getConfig().getString("mail.port"));
        this.props.put("mail.smtp.ssl.trust", this.plugin.getConfig().getString("mail.properties.mail.smtp.ssl.trust"));
    }

    public void sendEmail(String to, String subject, String text) {
        Session session = Session.getInstance(this.props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailSender.this.username, EmailSender.this.password);
            }
        });
        session.setDebug(this.plugin.getConfig().getBoolean("mail.debug"));

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(this.username));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
        } catch (MessagingException var6) {
            var6.printStackTrace();
            throw new RuntimeException(var6);
        }
    }
}
