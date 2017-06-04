package com.cisco.app.dbmigrator.migratorapp.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.cisco.app.dbmigrator.migratorapp.config.SyncConfig;
import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncEvent;
import com.cisco.app.dbmigrator.migratorapp.core.event.SyncMarker;
import com.cisco.app.dbmigrator.migratorapp.logging.codecs.SyncAttrs;
import com.cisco.app.dbmigrator.migratorapp.logging.dao.SyncEventDao;
import com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncNode;

public class Mailer {
	private final static SyncEventDao eventDao= new SyncEventDao();
	private Mailer() {
	}
	private final static Logger logger = Logger.getLogger(Mailer.class);
	public static final String FAILURE = "Failure";
	public static final String STARTED = "Started";
	public static final String COMPLETED = "Completed";
	public static final String CANCELLED = "Cancelled";

	public static void sendmail(String to, String cc, String subject, String body, String attachment) {
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", SyncConfig.INSTANCE.getMailerProperty("mail.smtp.host"));
		properties.setProperty("protocol", SyncConfig.INSTANCE.getMailerProperty("protocol"));
		properties.put("mail.smtp.port", SyncConfig.INSTANCE.getMailerProperty("mail.smtp.port"));
		Session session = Session.getDefaultInstance(properties);
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.setFrom(new InternetAddress(SyncConfig.INSTANCE.getMailerProperty("fromAlias"), "Sync Notification"));
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			InternetAddress[] toAdressArray = InternetAddress.parse(to);
			msg.setRecipients(Message.RecipientType.TO, toAdressArray);
			InternetAddress[] ccAdressArray = InternetAddress.parse(cc);
			msg.setRecipients(Message.RecipientType.CC, ccAdressArray);
			// Message body part
			Multipart multipart = new MimeMultipart();
			if (body != null) {
				multipart = setMultipartMsg("body", body, multipart);
			}
			if (attachment != null) {
				multipart = setMultipartMsg("attachment", attachment, multipart);
			}
			// Set the complete message parts
			msg.setContent(multipart);
			// Send message
			Transport.send(msg);
		} catch (MessagingException e) {
			logger.info("Exception while sending mail "+e);
		} catch (UnsupportedEncodingException e) {
			logger.info("Exception while sending mail "+e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void sendmail(ObjectId eventId, String attachment, Exception exception, String mailType) {
		SyncEvent event = eventDao.getEvent(eventId);
		if (mailType != null && (mailType.equalsIgnoreCase(STARTED) || mailType.equalsIgnoreCase(COMPLETED))) {
			SyncMarker marker = eventDao.getEventStats(eventId);
			event.setMarker(marker);
		}
		sendmail(event, attachment, exception, mailType);
	}

	@SuppressWarnings("rawtypes")
	public static void sendmail(SyncEvent event, String attachment, Exception exception, String mailType) {
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", "outbound.cisco.com");
		properties.setProperty("protocol", "smtp");
		properties.put("mail.smtp.port", "25");
		Session session = Session.getDefaultInstance(properties);
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.setFrom(new InternetAddress("donotreply@cisco.com", "Sync Notification"));
			msg.setSentDate(new Date());
			String toAliases = event.getNotifIds();
			if(toAliases==null || toAliases.isEmpty()){
				toAliases = SyncConfig.INSTANCE.getMailerProperty("teamAlias");
			}
			InternetAddress[] toAdressArray = InternetAddress.parse(toAliases);
			msg.setRecipients(Message.RecipientType.TO, toAdressArray);
			InternetAddress[] ccAdressArray = InternetAddress.parse(SyncConfig.INSTANCE.getMailerProperty("teamAlias"));
			msg.setRecipients(Message.RecipientType.CC, ccAdressArray);
			// Message body part
			Multipart multipart = new MimeMultipart();
			if (attachment != null) {
				multipart = setMultipartMsg("attachment", attachment, multipart);
			}
			if (mailType != null) {
				if (mailType.equalsIgnoreCase(FAILURE)) {
					msg.setSubject(getLifeCycle()+" Event Failure Notification", "UTF-8");
					multipart = readEmailFromHtml("/mailTemplate/failureEventMailTemplate.html", multipart, event,
							exception, mailType);
				} else if (mailType.equalsIgnoreCase(STARTED)) {
					msg.setSubject(getLifeCycle()+" Event Start Notification ", "UTF-8");
					multipart = readEmailFromHtml("/mailTemplate/startedEventMailTemplate.html", multipart, event,
							exception, mailType);
				} else if(mailType.equalsIgnoreCase(CANCELLED)){
					msg.setSubject(getLifeCycle()+" Event Cancel Notification ", "UTF-8");
					multipart = readEmailFromHtml("/mailTemplate/cancelledEventMailTemplate.html", multipart, event, exception,mailType);
				} else {
					msg.setSubject(getLifeCycle()+" Event Complete Notification ", "UTF-8");
					multipart = readEmailFromHtml("/mailTemplate/completedEventMailTemplate.html", multipart, event,
							exception, mailType);
				}
			}
			// Set the complete message parts
			msg.setContent(multipart);
			// Send message
			Transport.send(msg);
			// System.out.println("Mail sent successfully");
		} catch (MessagingException e) {
			logger.info("Exception while sending mail "+e);
		} catch (UnsupportedEncodingException e) {
			logger.info("Exception while sending mail "+e);
		}
	}

	private static Multipart setMultipartMsg(String type, String data, Multipart multipart) {
		BodyPart messageBodyPart = new MimeBodyPart();
		try {
			if (type.equalsIgnoreCase("attachment")) {
				DataSource ds;
				try {
					ds = new ByteArrayDataSource(data, "application/x-any");
					messageBodyPart.setDataHandler(new DataHandler(ds));
					messageBodyPart.setFileName("eventDoc.txt");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			multipart.addBodyPart(messageBodyPart);
		} catch (MessagingException e) {
			logger.info("Exception While Setting Message Body "+e);
		}
		return multipart;
	}

	@SuppressWarnings("rawtypes")
	private static Multipart readEmailFromHtml(String filePath, Multipart multipart, SyncEvent event, Exception ex,
			String mailType) {
		BodyPart messageBodyPart = new MimeBodyPart();
		String msg = readContentFromFile(filePath);
		try {
			// Set key values
			Map<String, String> input = new HashMap<String, String>();
			input.put(SyncAttrs.EVENT_NAME, event.getEventName());
			input.put(SyncAttrs.EVENT_ID, event.getEventId().toString());
			input.put(SyncAttrs.MAP_NAME, event.getMapName());
			input.put(SyncAttrs.MAP_ID, event.getMapId().toString());
			input.put(SyncAttrs.EVENT_TYPE, event.getEventType().toString());
			input.put(SyncAttrs.HOST_NAME, getHostName());
			if (mailType.equalsIgnoreCase(FAILURE)) {
				input.put(SyncAttrs.TRACE, ExceptionUtils.getStackTrace(ex));
			} else if (mailType.equalsIgnoreCase(STARTED)) {
				input.put(SyncAttrs.START_TIME, new Date(System.currentTimeMillis()).toString());
			} else if(mailType.equalsIgnoreCase(COMPLETED)){
				if(event.getMarker()!=null){
					input.put(SyncAttrs.ROWS_READ, Integer.toString(event.getMarker().getRowsDumped()));
					input.put(SyncAttrs.ROWS_DUMPED, Integer.toString(event.getMarker().getRowsDumped()));
					input.put(SyncAttrs.TOTAL_ROWS, Integer.toString(event.getMarker().getTotalRows()));
					input.put(SyncAttrs.START_TIME, event.getMarker().getStartTime().toString());
					input.put(SyncAttrs.END_TIME, event.getMarker().getEndTime().toString());
					input.put(SyncAttrs.DURATION,
							formatDate(event.getMarker().getStartTime(), event.getMarker().getEndTime()));
				}
			}
			input.put("DATE", new Date().toString());
			Set<Entry<String, String>> entries = input.entrySet();
			for (Map.Entry<String, String> entry : entries) {
				msg = msg.replace(entry.getKey().trim(), entry.getValue() !=null ? entry.getValue().trim() : "-");
			}
		} catch (Exception exception) {
			logger.info("Exception While Setting The Values For Html Fields "+exception);
			exception.printStackTrace();
		}
		try {
			messageBodyPart.setContent(msg, "text/html");
			multipart.addBodyPart(messageBodyPart);
		} catch (MessagingException e) {
			logger.info("Exception While Setting Message Body "+e);
		}
		return multipart;
	}

	private static String readContentFromFile(String fileName) {
		String contents = "";
		InputStream is = null;//Mailer.class.getResourceAsStream(fileName);
		try {
			is = Mailer.class.getResourceAsStream(fileName);
			contents = IOUtils.toString(is);
		} catch (IOException e) {
			logger.info("Exception While Reading The Html Content "+e);
		}
		return contents;
	}

	private static String formatDate(Date sDate, Date eDate) {
		long duration = eDate.getTime() - sDate.getTime();
		long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
		duration = duration%60;
		long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		duration = duration%60;
		long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);		
		return diffInHours + "Hrs : " + diffInMinutes + " Mins : " + diffInSeconds + " Sec.";
	}
	private static String getHostName() {
		InetAddress localHost = null;
		try {
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			logger.error("Error while getting host name", e);
		}
		return localHost != null ? localHost.getHostName() : null;
	}
	private static String getLifeCycle() {
		String lifeCycle = SyncConfig.INSTANCE.getDbProperty(SyncConstants.LIFE);
		if(lifeCycle==null){
			lifeCycle="local";
		}
		return lifeCycle.toUpperCase();
	}
	
	public static void sendMailForFailedNode(SyncNode node) {
		
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", SyncConfig.INSTANCE.getMailerProperty("mail.smtp.host"));
		properties.setProperty("protocol", SyncConfig.INSTANCE.getMailerProperty("protocol"));
		properties.put("mail.smtp.port", SyncConfig.INSTANCE.getMailerProperty("mail.smtp.port"));
		Session session = Session.getDefaultInstance(properties);
		try {
			BodyPart messageBodyPart = new MimeBodyPart();
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.setFrom(new InternetAddress(SyncConfig.INSTANCE.getMailerProperty("fromAlias"), "Sync Notification"));
			msg.setSentDate(new Date());
			String toAliases = SyncConfig.INSTANCE.getMailerProperty("teamAlias");
			InternetAddress[] toAdressArray = InternetAddress.parse(toAliases);
			msg.setRecipients(Message.RecipientType.TO, toAdressArray);
			Multipart multipart = new MimeMultipart();
			msg.setSubject(getLifeCycle()+" Failed Node Notification", "UTF-8");
			String content = readContentFromFile("/mailTemplate/failedNodeMailTemplate.html");
			try {
				Map<String, String> input = new HashMap<String, String>();
				input.put(SyncAttrs.HOST_NAME, node.getHostName());
				input.put(SyncAttrs.NODE_NAME, node.getNodeName());
				input.put(SyncAttrs.JVM, node.getJvmName());
				input.put(SyncAttrs.LIFE_CYCLE, node.getLifeCycle());
				input.put(SyncAttrs.UUID, node.getUUID());
				if(String.valueOf(node.getLastPingTime()) !=null && node.getLastPingTime() != 0)
					input.put(SyncAttrs.LAST_PING_TIME, String.valueOf(new Date(node.getLastPingTime())));
				else
					input.put(SyncAttrs.LAST_PING_TIME, "-");
				if(String.valueOf(node.getFailureTime()) !=null && node.getFailureTime() != 0)
					input.put(SyncAttrs.FAILURE_TIME, String.valueOf(new Date(node.getFailureTime())));
				else
					input.put(SyncAttrs.FAILURE_TIME, "-");
				input.put("DATE", new Date().toString());
				Set<Entry<String, String>> entries = input.entrySet();
				for (Map.Entry<String, String> entry : entries) {
					content = content.replace(entry.getKey().trim(), entry.getValue() !=null ? entry.getValue().trim() : "-");
				}
			} catch (Exception exception) {
				logger.info("Exception While Setting The Values For Html Fields "+exception);
				exception.printStackTrace();
			}
			try {
				messageBodyPart.setContent(content, "text/html");
				multipart.addBodyPart(messageBodyPart);
			} catch (MessagingException e) {
				logger.info("Exception While Setting Message Body For Failed Node"+e);
			}
			msg.setContent(multipart);
			Transport.send(msg);
		} catch (MessagingException e) {
			logger.info("Exception while sending mail for failed node "+e);
		} catch (UnsupportedEncodingException e) {
			logger.info("Exception while sending mail  for failed node "+e);
		}
	}
}
