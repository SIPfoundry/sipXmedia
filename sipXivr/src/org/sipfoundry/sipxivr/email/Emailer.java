/*
 *
 *
 * Copyright (C) 2009 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 */

package org.sipfoundry.sipxivr.email;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sipfoundry.commons.userdb.User;
import org.sipfoundry.commons.userdb.User.EmailFormats;
import org.sipfoundry.voicemail.mailbox.Folder;
import org.sipfoundry.voicemail.mailbox.MailboxManager;
import org.sipfoundry.voicemail.mailbox.VmMessage;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;

public class Emailer implements ApplicationContextAware {
    private static final Logger LOG = Logger.getLogger("org.sipfoundry.sipxivr");
    private ExecutorService m_es;
    private Session m_session;
    private ApplicationContext m_context;
    private String m_audioFormat;
    private MailboxManager m_mailboxManager;

    public void init() {
        m_es = Executors.newCachedThreadPool();
        // Setup mail server
        Properties props = System.getProperties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.user", "postmaster"); // TODO get from ivrConfig
        m_session = Session.getDefaultInstance(props, null);
    }

    /**
     * Queue up sending the VmMessage as an e-mail to the addresses specified in the mailbox
     *
     * @param mailbox
     * @param vmessage
     */
    public void queueVm2Email(User destUser, VmMessage vmessage) {
        if (destUser.getEmailFormat() != EmailFormats.FORMAT_NONE
                || destUser.getAltEmailFormat() != EmailFormats.FORMAT_NONE) {
            if (vmessage.getParentFolder().equals(Folder.CONFERENCE)) {
                LOG.info("Emailer::do not queue email for conferences");
                return;
            }
            LOG.info("Emailer::queueVm2Email queuing e-mail for " + destUser.getIdentity());
            BackgroundVMMailer bm = new BackgroundVMMailer(destUser, vmessage);
            submit(bm);
        }
    }
    
    /**
     * Queue up sending info on miss call as an e-mail to the addresses specified in the mailbox
     *
     * @param mailbox
     * @param vmessage
     */
    public void queueMissCallEmail(User destUser, String fromUri) {
        if (destUser.isNotifyMissCalls() &&
                (destUser.getEmailFormat() != EmailFormats.FORMAT_NONE
                || destUser.getAltEmailFormat() != EmailFormats.FORMAT_NONE))  {
            LOG.info("Emailer::queueMissCallEmail queuing e-mail for " + destUser.getIdentity());
            BackgroundMissCallMailer bm = new BackgroundMissCallMailer(destUser, fromUri);
            submit(bm);
        }
    }

    /**
     * The Runnable class that builds and sends the e-mail
     */
    class BackgroundVMMailer implements Runnable {
        VmMessage m_vmessage;
        User m_user;

        BackgroundVMMailer(User destUser, VmMessage vmessage) {
            m_vmessage = vmessage;
            m_user = destUser;
        }

        /**
         * Build up the MIME multipart formatted e-mail
         *
         * @param attachAudio Attach the audio file as part of the message
         * @return
         * @throws AddressException
         * @throws MessagingException
         */
        javax.mail.Message buildMessage(EmailFormatter emf, boolean attachAudio) throws AddressException,
                MessagingException, IOException {
            MimeMessage message = new MimeMessage(m_session);
            message.setFrom(new InternetAddress(emf.getSender()));

            message.setSubject(emf.getSubject(), "UTF-8");

            message.addHeader("X-SIPX-FROMURI", m_vmessage.getDescriptor().getFromUri());
            message.addHeader("X-SIPX-MSGID", m_vmessage.getMessageId());
            message.addHeader("X-SIPX-MBXID", m_user.getUserName());
            message.addHeader("X-SIPX-MSG", "yes");

            String htmlBody = emf.getHtmlBody();
            String textBody = emf.getTextBody();

            // Load transcript email body templates if user is configured to transcribe audio
            // and email template is provided for transcript
            if (m_user.isTranscribeVoicemail()) {
                htmlBody = !StringUtils.isBlank(emf.getHtmlTranscript()) ? emf.getHtmlTranscript() : htmlBody;
                textBody = !StringUtils.isBlank(emf.getTextTranscript()) ? emf.getTextTranscript() : textBody;
            }

            // Use multipart/alternative if we are attaching audio or there is an html body part
            if (attachAudio || htmlBody != null && htmlBody.length() > 0) {
                // Create an "mulipart/alternative" part with text and html alternatives
                MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);

                if (textBody != null && textBody.length() > 0) {
                    // Add the text part of the message first
                    helper.setText(textBody); // UTF-8 in case there's Unicode in there
                }
                
                if (htmlBody != null && htmlBody.length() > 0) {
                    // Add the HTML part of the message
                    helper.setText(htmlBody, true);

                    if (m_user.getEmailFormat().equals(EmailFormats.FORMAT_FULL)) {
                        // Add the IMAGEs part of the message
                        insertMimeImage("images/play_50x50.png", "imageListen", helper);
                        insertMimeImage("images/inbox_50x50.png", "imageInbox", helper);
                        insertMimeImage("images/delete_50x50.png", "imageDelete", helper);
                    }
                }

                // Add the audio file as an attachment
                if (attachAudio) {

                    File file = m_vmessage.getAudioFile();

                    DataSource dataSource = new FileDataSource(file) {
                        @Override
                        public String getContentType() {
                            return getMimeType();
                        }
                    };

                    helper.addAttachment(file.getName(), dataSource);
                }


            } else {
                if (textBody != null && textBody.length() > 0) {
                    // Just a text part, use a simple message
                    message.setText(textBody, "UTF-8");
                }
            }

            if (m_vmessage.isUrgent()) {
                message.setHeader("X-Priority", "1");
            }
            return message;
        }

        private String getMimeType() {
            if (m_audioFormat.equals("mp3")) {
                return "audio/mpeg";
            }
            return "audio/x-wav";
        }

        private void insertMimeImage(String imageSource, String imageKey, MimeMessageHelper helper)
                throws MessagingException, IOException {
            ClassPathResource res = new ClassPathResource(imageSource);
            helper.addInline(imageKey, res);
        }

        /**
         * Build and send the message as e-mails to the recipients
         */
        @Override
        public void run() {
            String to = m_user.getEmailAddress();
            String alt = m_user.getAltEmailAddress();

            LOG.debug("Emailer::run started (voicemail)");
            
            EmailFormats fmt = m_user.getEmailFormat();
            // Send to the main e-mail address
            if (fmt != EmailFormats.FORMAT_NONE) {
                try {
                     boolean attachAudio = m_user.isAttachAudioToEmail();
                    LOG.info(String.format("Emailer::run sending message %s as %s email to %s %s audio",
                            m_vmessage.getMessageId(), fmt.toString(), to, attachAudio ? "with" : "without"));
                    EmailFormatter emf = getEmailFormatter(fmt, m_user, m_vmessage);
                    javax.mail.Message message = buildMessage(emf, attachAudio);
                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
                    Transport.send(message);
                    if (attachAudio && m_user.isForwardDeleteVoicemail()) {
                        m_mailboxManager.deleteMessage(m_user, m_vmessage);
                    }                    
                } catch (Exception e) {
                    LOG.error("Emailer::run problem sending email.", e);
                }
            }

            // Send to the alternate e-mail address
            // (this could be a bit better: if both main and alternative have the same value for
            // fmt and isAttachVoicemailxxx, one could create the message once and send it with
            // two recipients)
            fmt = m_user.getAltEmailFormat();
            if (fmt != EmailFormats.FORMAT_NONE) {
                try {
                    boolean attachAudio = m_user.isAltAttachAudioToEmail();
                    LOG.info(String.format("Emailer::run sending message %s as %s email to %s %s audio",
                            m_vmessage.getMessageId(), fmt.toString(), alt, attachAudio ? "with" : "without"));
                    EmailFormatter emf = getEmailFormatter(fmt, m_user, m_vmessage);
                    javax.mail.Message message = buildMessage(emf, attachAudio);

                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(alt));
                    Transport.send(message);
                    if (attachAudio && m_user.isForwardDeleteVoicemail()) {
                        m_mailboxManager.deleteMessage(m_user, m_vmessage);
                    }                    
                } catch (Exception e) {
                    LOG.error("Emailer::run problem sending alternate email.", e);
                }
            }
            m_vmessage.cleanup();
            LOG.debug("Emailer::run finished (voicemail)");
        }
    }
    
    class BackgroundMissCallMailer implements Runnable {
        String m_fromUri;
        User m_user;
        
        public BackgroundMissCallMailer(User user, String fromUri) {
            this.m_user = user;
            this.m_fromUri = fromUri;
        }
        
        /**
         * Build up the MIME multipart formatted e-mail
         *
         * @param attachAudio Attach the audio file as part of the message
         * @return
         * @throws AddressException
         * @throws MessagingException
         */
        javax.mail.Message buildMessage(EmailFormatter emf) throws AddressException,
                MessagingException, IOException {
            MimeMessage message = new MimeMessage(m_session);
            message.setFrom(new InternetAddress(emf.getSenderMissCall()));

            message.setSubject(emf.getSubjectMissCall(), "UTF-8");

            message.addHeader("X-SIPX-FROMURI", m_fromUri);
            message.addHeader("X-SIPX-MBXID", m_user.getUserName());
            message.addHeader("X-SIPX-MSG", "yes");

            String htmlBody = emf.getHtmlMissCall();
            String textBody = emf.getTextMissCall();

            // Use multipart/alternative if we are attaching audio or there is an html body part
            if (htmlBody != null && htmlBody.length() > 0) {
                // Create an "mulipart/alternative" part with text and html alternatives
                MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);

                if (textBody != null && textBody.length() > 0) {
                    // Add the text part of the message first
                    helper.setText(textBody); // UTF-8 in case there's Unicode in there
                }
                
                if (htmlBody != null && htmlBody.length() > 0) {
                    // Add the HTML part of the message
                    helper.setText(htmlBody, true);
                }

            } else {
                if (textBody != null && textBody.length() > 0) {
                    // Just a text part, use a simple message
                    message.setText(textBody, "UTF-8");
                }
            }

            return message;
        }

        @Override
        public void run() {
            String to = m_user.getEmailAddress();
            String alt = m_user.getAltEmailAddress();

            LOG.debug("Emailer::run started (miss call)");
            
            EmailFormats fmt = m_user.getEmailFormat();
            // Send to the main e-mail address
            if (fmt != EmailFormats.FORMAT_NONE) {
                try {
                    LOG.info(String.format("Emailer::run sending miss call message as %s email to %s",
                            fmt.toString(), to));
                    EmailFormatter emf = getEmailFormatter(fmt, m_user, m_fromUri);
                    javax.mail.Message message = buildMessage(emf);
                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
                    Transport.send(message);
                } catch (Exception e) {
                    LOG.error("Emailer::run problem sending email.", e);
                }
            }

            // Send to the alternate e-mail address
            fmt = m_user.getAltEmailFormat();
            if (fmt != EmailFormats.FORMAT_NONE) {
                try {
                    LOG.info(String.format("Emailer::run sending miss call message as %s email to %s",
                            fmt.toString(), alt));
                    EmailFormatter emf = getEmailFormatter(fmt, m_user, m_fromUri);
                    javax.mail.Message message = buildMessage(emf);
                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(alt));
                    Transport.send(message);
                } catch (Exception e) {
                    LOG.error("Emailer::run problem sending alternate email.", e);
                }
            }
            LOG.debug("Emailer::run finished (miss call)");
        }
    }

    /**
     * Factory method to return the right EmailFormatter based on emailFormat
     * @param emailFormat
     * @param ivrConfig
     * @param mailbox
     * @param vmessage
     * @return
     */
    private EmailFormatter getEmailFormatter(EmailFormats emailFormat, User user, VmMessage vmessage) {
        EmailFormatter formatter = m_context.getBean(emailFormat.getId(), EmailFormatter.class);
        formatter.init(user, vmessage);
        try {
            formatter.tryTranscribe(vmessage);
        } catch (Exception e) {
            LOG.error("Emailer::run problem transcribing audio.", e);
        }
        return formatter;
    }
    
    private EmailFormatter getEmailFormatter(EmailFormats emailFormat, User user, String fromUri) {
        EmailFormatter formatter = m_context.getBean(emailFormat.getId(), EmailFormatter.class);
        formatter.init(user, fromUri);
        return formatter;
    }

    public void submit(BackgroundVMMailer bm) {
        m_es.submit(bm);
    }
    
    public void submit(BackgroundMissCallMailer bm) {
        m_es.submit(bm);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        m_context = context;
    }

    public void setAudioFormat(String format) {
        m_audioFormat = format;
    }

    @Required
    public void setMailboxManager(MailboxManager mgr) {
        m_mailboxManager = mgr;
    }    
}