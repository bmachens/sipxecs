/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.bulk.ldap;

import java.util.Collection;
import java.util.Map;

import org.dbunit.dataset.ITable;
import org.sipfoundry.sipxconfig.TestHelper;
import org.sipfoundry.sipxconfig.TestHelper.TestCaseDb;
import org.sipfoundry.sipxconfig.admin.CronSchedule;
import org.sipfoundry.sipxconfig.bulk.ldap.LdapSystemSettings.AuthenticationOptions;
import org.springframework.context.ApplicationContext;

public class LdapManagerTestDb extends TestCaseDb {

    private LdapManager m_context;
    LdapConnectionParams m_params = null;
    AttrMap m_attrMap = null;

    @Override
    protected void setUp() throws Exception {
        ApplicationContext appContext = TestHelper.getApplicationContext();
        m_context = (LdapManager) appContext.getBean(LdapManager.CONTEXT_BEAN_NAME);
        m_params = m_context.createConnectionParams();
        m_attrMap = m_context.createAttrMap();
        TestHelper.cleanInsert("ClearDb.xml");
        m_params.setHost("abc");
        m_params.setPort(1234);
        m_params.setPrincipal("principal");
        m_params.setSecret("secret");

        m_context.setConnectionParams(m_params);
        m_attrMap.setUniqueId(m_params.getId());
        m_context.setAttrMap(m_attrMap);
    }

    public void testLdapSystemSettings() throws Exception {
        LdapSystemSettings settings = m_context.getSystemSettings();
        assertNotNull(settings);
        ITable before = TestHelper.getConnection().createDataSet().getTable("ldap_settings");
        assertEquals(0, before.getRowCount());
        settings.setAuthenticationOptions(AuthenticationOptions.LDAP);
        settings.setEnableOpenfireConfiguration(true);
        m_context.saveSystemSettings(settings);
        ITable after = TestHelper.getConnection().createDataSet().getTable("ldap_settings");
        assertEquals(1, after.getRowCount());
        assertEquals("LDAP", after.getValue(0, "authentication_options"));
        assertTrue((Boolean)after.getValue(0, "enable_openfire_configuration"));
        //by default LDAP configuration is not activated
        assertFalse((Boolean)after.getValue(0, "configured"));
        //test noLDAP / unconfigured
        settings.setAuthenticationOptions(AuthenticationOptions.NO_LDAP);
        settings.setEnableOpenfireConfiguration(true);
        settings.setConfigured(false);
        m_context.saveSystemSettings(settings);
        after = TestHelper.getConnection().createDataSet().getTable("ldap_settings");
        assertEquals(1, after.getRowCount());
        assertEquals("noLDAP", after.getValue(0, "authentication_options"));
        assertFalse((Boolean)after.getValue(0, "configured"));
        //test pinLDAP
        settings.setAuthenticationOptions(AuthenticationOptions.PIN_LDAP);
        settings.setEnableOpenfireConfiguration(true);
        m_context.saveSystemSettings(settings);
        after = TestHelper.getConnection().createDataSet().getTable("ldap_settings");
        assertEquals(1, after.getRowCount());
        assertEquals("pinLDAP", after.getValue(0, "authentication_options"));
    }

    public void testGetConnectionParams() throws Exception {
        LdapConnectionParams connectionParams = m_context.getConnectionParams(m_params.getId());
        assertNotNull(connectionParams);
        assertNotNull(connectionParams.getSchedule());

        ITable ldapConnectionTable = TestHelper.getConnection().createDataSet().getTable("ldap_connection");
        assertEquals(1, ldapConnectionTable.getRowCount());

        ITable cronScheduleTable = TestHelper.getConnection().createDataSet().getTable("cron_schedule");
        // this will change once we start keeping something else in the table
        assertEquals(1, cronScheduleTable.getRowCount());
    }

    public void testSetConnectionParams() throws Exception {
        ITable ldapConnectionTable = TestHelper.getConnection().createDataSet().getTable("ldap_connection");
        assertEquals(1, ldapConnectionTable.getRowCount());

        assertEquals("secret", ldapConnectionTable.getValue(0, "secret"));
        assertEquals(1234, ldapConnectionTable.getValue(0, "port"));
        assertEquals("principal", ldapConnectionTable.getValue(0, "principal"));
        assertEquals("abc", ldapConnectionTable.getValue(0, "host"));
    }

    public void testGetAttrMap() throws Exception {
        AttrMap attrMap = m_context.getAttrMap(m_params.getId());
        assertNotNull(attrMap);

        ITable attrMapTable = TestHelper.getConnection().createDataSet().getTable("ldap_attr_map");
        assertEquals(1, attrMapTable.getRowCount());

        ITable userToLdapTable = TestHelper.getConnection().createDataSet().getTable(
                "ldap_user_property_to_ldap_attr");
        assertTrue(1 < userToLdapTable.getRowCount());

        TestHelper.cleanInsertFlat("bulk/ldap/ldap_attr_map.db.xml");
        attrMap = m_context.getAttrMap(1000);
        Map<String, String> userToLdap = attrMap.getUserToLdap();
        assertEquals(2, userToLdap.size());

        assertEquals("uid", attrMap.getAttribute("username"));
        assertEquals("cn", attrMap.getAttribute("firstname"));

        assertEquals("filter", attrMap.getFilter());
        Collection<String> selectedObjectClasses = attrMap.getSelectedObjectClasses();

        assertEquals(2, selectedObjectClasses.size());
        assertTrue(selectedObjectClasses.contains("abc"));
        assertTrue(selectedObjectClasses.contains("def"));
    }

    public void testSetAttrMap() throws Exception {
        AttrMap attrMap = m_context.getAttrMap(m_params.getId());
        attrMap.setFilter("ou=marketing");
        assertNotNull(attrMap);

        attrMap.setAttribute("a", "1");
        attrMap.setAttribute("b", "2");

        m_context.setAttrMap(attrMap);

        ITable attrMapTable = TestHelper.getConnection().createDataSet().getTable("ldap_attr_map");
        assertEquals(1, attrMapTable.getRowCount());
        assertEquals("ou=marketing", attrMapTable.getValue(0, "filter"));

        ITable userToLdapTable = TestHelper.getConnection().createDataSet().getTable(
                "ldap_user_property_to_ldap_attr");
        assertTrue(1 < userToLdapTable.getRowCount());
    }

    public void testSetSchedule() throws Exception {
        CronSchedule schedule = new CronSchedule();
        schedule.setType(CronSchedule.Type.HOURLY);
        schedule.setMin(15);

        m_context.setSchedule(schedule, m_params.getId());

        assertEquals(1, TestHelper.getConnection().getRowCount("cron_schedule",
                "where cron_string = '0 15 * ? * *'"));
    }

    public void testSetScheduleEnabled() throws Exception {
        CronSchedule schedule = new CronSchedule();
        schedule.setType(CronSchedule.Type.WEEKLY);
        schedule.setDayOfWeek(4);
        schedule.setMin(15);
        schedule.setEnabled(true);

        m_context.setSchedule(schedule, m_params.getId());

        assertEquals(1, TestHelper.getConnection().getRowCount("cron_schedule",
                "where cron_string = '0 15 0 ? * 4' and enabled = 'true'"));
    }

    public void testGetSetSchedule() throws Exception {
        CronSchedule schedule = m_context.getSchedule(m_params.getId());
        m_context.setSchedule(schedule, m_params.getId());
    }
}