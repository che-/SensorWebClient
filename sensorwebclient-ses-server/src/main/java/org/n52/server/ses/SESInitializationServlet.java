/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.server.ses;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.swing.JOptionPane;

import org.n52.server.ses.eml.Meta_Builder;
import org.n52.server.ses.hibernate.HibernateUtil;
import org.n52.server.ses.service.SesSensorServiceImpl;
import org.n52.server.ses.service.SesUserServiceImpl;
import org.n52.server.ses.util.SesUtil;
import org.n52.server.ses.util.WnsUtil;
import org.n52.shared.serializable.pojos.BasicRule;
import org.n52.shared.serializable.pojos.User;
import org.n52.shared.serializable.pojos.UserDTO;
import org.n52.shared.serializable.pojos.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup unit of the SES-Client. Checks connections to SES and WNS.
 * 
 * FIXME Refactor! Avoid using servlet!
 */
public class SESInitializationServlet extends HttpServlet {

    private static final long serialVersionUID = -8453052195694079440L;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SESInitializationServlet.class);

    public static boolean SESavailable = false;

    public static boolean WNSavailable = false;

    public static boolean initialized = false;

    @Override
    public void init() throws ServletException {
        
        try {
            LOGGER.debug("Initialize " + getClass().getName() +" Servlet for SES Client");

            Config.init(this.getServletContext().getRealPath("/"));
            Config.USER_NAME = this.getServletContext().getInitParameter("MAIL_USERNAME");
            Config.PASSWORD = this.getServletContext().getInitParameter("MAIL_PASSWORD");
            Config.SENDER_ADDRESS = this.getServletContext().getInitParameter("MAIL_SENDER_ADDRESS");
            Config.SMTP_HOST = this.getServletContext().getInitParameter("MAIL_SMTP_HOST");
            Config.STARTTLS_ENABLE = this.getServletContext().getInitParameter("MAIL_STARTTLS_ENABLE");
            Config.PORT = this.getServletContext().getInitParameter("MAIL_PORT");
            Config.AUTH = this.getServletContext().getInitParameter("MAIL_AUTH");
            Config.SSL_ENABLE = this.getServletContext().getInitParameter("MAIL_SSL_ENABLE");

            LOGGER.info("ckeck availability of SES and WNS");
            Thread t = new Thread(new Runnable() {
                public void run() {
                    checkAvailability();
                }
            });
            t.start();

            // init the servlet sesUserService. This servlet handle user registrations
            // and creation
            LOGGER.info("init sesUserService");
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    sesUserService();
                }
            });
            t2.start();
        } catch (Exception e) {
            LOGGER.error("Could not initialize servlet appropriatly", e);
            return;
        }

        // validate templates
        LOGGER.info("Validate templates");
        Thread validationThread = new Thread(new Runnable() {
            public void run() {
                try {
                    templatesValidation();
                } catch (Exception e) {
                    LOGGER.error("Error validating template", e);
                }
            }
        });
        validationThread.start();
    }

    /**
     * This method checks all 20 seconds the availability of the SES and the WNS.
     * After both services are available, the registered sensors from SES are stored in DB
     */
    private void checkAvailability() {
        Thread checkThread = new Thread(new Runnable() {
            @SuppressWarnings("static-access")
            public void run() {
                while (!SESInitializationServlet.SESavailable || !SESInitializationServlet.WNSavailable) {
                    try {
                        // check if SES is available
                        if (!SESInitializationServlet.SESavailable) {
                            SESInitializationServlet.SESavailable = SesUtil.isAvailable();
                            LOGGER.trace("SES (\"" + Config.sesEndpoint + "\") is available = " + SESInitializationServlet.SESavailable);
                        }

                        // check if WNS is available
                        if (!SESInitializationServlet.WNSavailable) {
                            SESInitializationServlet.WNSavailable = WnsUtil.isAvailable();
                            LOGGER.trace("WNS (\"" + Config.wns + "\") is available = " + SESInitializationServlet.WNSavailable);
                        }
                        // check all 20 seconds
                        Thread.yield();
                        Thread.currentThread().sleep(20000);
                    } catch (InterruptedException e) {
                        LOGGER.trace("Checking service was interrupted.", e);
                    }
                }
                
                // set InitServlet to TRUE
                SESInitializationServlet.initialized = true;

                // add sensors to database after initialization
                LOGGER.debug("add Sensors to DB");
                SesSensorServiceImpl.addSensorsToDB(); // XXX
            }
        });
        checkThread.run();
    }

    /**
     * This method cheks whether it is possible to build rules without exceptions.
     * If one exception is thrown --> template is not valid
     */
    private void templatesValidation() throws Exception {
    	// wait until servlet is initialized
        while (!SESInitializationServlet.initialized) {
            Thread.yield();
        }
        String ruleName = "DUMMY_RULE";
        String medium = "E-Mail";

        // create dummy user
        User dummyUser = new User();
        dummyUser.setWnsEmailId("999999");

        // create dummy basic Rule
        BasicRule dummyBasicrule = new BasicRule();
        dummyBasicrule.setName(ruleName);

        try {
            Meta_Builder.createTextMeta(dummyUser, ruleName, medium);
            Meta_Builder.createTextFailureMeta(dummyUser, dummyBasicrule, medium, "dummySensor");
            Meta_Builder.createXMLMeta(dummyUser, ruleName, medium, "XML");
            Meta_Builder.createXMLMeta(dummyUser, ruleName, medium, "EML");
        } catch (Exception e) {
            LOGGER.error("Template validation failed! Please change the templates and restart the application", e);
            SESInitializationServlet.initialized = false;
            JOptionPane.showMessageDialog(null, Config.adminMessage);
        }

    }

    private void sesUserService() {

        Thread sesUserThread = new Thread(new Runnable() {
            public void run() {
                while (!SESInitializationServlet.initialized) {
                    try {
                        // check all 20 seconds
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        LOGGER.trace("Checking service was interrupted.", e);
                    }
                }
                // run only if the init servlet is initialized
                if (SESInitializationServlet.initialized) {
                    LOGGER.info("create default user");

                    // create default admin on start
                    UserDTO admin =
                            SesUserServiceImpl.createUserDTO(new User("admin", "Admin", createMD5("admin"),
                                    Config.SENDER_ADDRESS, "", UserRole.ADMIN, true));
                    admin.setRegisterID(UUID.randomUUID().toString());

                    // check if default admin already exists
                    if (!HibernateUtil.existsUserName(admin.getUserName())) {
                        try {
                            LOGGER.debug("get IDs from WNS for admin");
                            admin.setWnsEmailId(WnsUtil.sendToWNSMail(admin.getName(), admin.geteMail()));
//                            admin.setWnsSmsId(WnsUtil
//                                    .sendToWNSSMS(admin.getName(), String.valueOf(admin.getHandyNr())));

                            HibernateUtil.addUser(new User(admin));
                        } catch (Exception e) {
                            LOGGER.debug("WNS is not available.", e);
                        }
                    } else {
                        LOGGER.debug("default admin already exists");
                    }

                    // in debug-mode. check if default user already exists
                    if (Config.debug) {
                        UserDTO user =
                            SesUserServiceImpl.createUserDTO(new User("user", "User", createMD5("user"),
                                    "52n.development@googlemail.com", "+456", UserRole.USER, true));
                        if (!HibernateUtil.existsUserName(user.getUserName())) {
                            user.setRegisterID(UUID.randomUUID().toString());

                            try {
                                user.setWnsEmailId(WnsUtil.sendToWNSMail(user.getName(), user.geteMail()));
                                user.setWnsSmsId(WnsUtil.sendToWNSSMS(user.getName(), String
                                        .valueOf(user.getHandyNr())));

                                HibernateUtil.addUser(new User(user));
                            } catch (Exception e) {
                                LOGGER.debug("WNS is not available.",e);
                            }
                        }
                    }
                }
                return;
            }
        });
        // start thread
        sesUserThread.run();
    }

    @Override
    public void destroy() {
        try {
            // Destroy method is called to undeploy the servlet.
            // To avoid redeploy hang-ups we have to close the hibernate session factory 
            HibernateUtil.getSessionFactory().close();
        } catch (Exception e) {
            LOGGER.error("Could not close database session factory appropriatly.", e);
        }

        super.destroy();
    }

    /**
     * create MD5 hash to code the password
     */
    public static String createMD5(String password) {
        StringBuffer buffer = new StringBuffer();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(password.getBytes());

            byte[] result = md5.digest();

            for (int i = 0; i < result.length; i++) {
                buffer.append(Integer.toHexString(0xFF & result[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Unkown MD5 algorithm.", e);
        }
        return buffer.toString();
    }
}