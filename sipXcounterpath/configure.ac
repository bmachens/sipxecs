AC_PREREQ(2.57)
AC_INIT(sipXcounterpath, 16.02, sipx-dev@list.sipfoundry.org)
AC_CONFIG_AUX_DIR(config)
m4_include([config/general.m4])
m4_include([config/sipXlib.m4])
m4_include([config/java.m4])
m4_include([config/java2.m4])
AM_INIT_AUTOMAKE(foreign tar-ustar)
AC_CONFIG_SRCDIR([device])
SFAC_INIT_FLAGS
AC_PROG_JAVA_CC([javac])
AC_PROG_JAVA([java])
PROG_JAVA_DEP
AC_CONFIG_FILES([
  Makefile
  etc/Makefile
  device/Makefile
  device/src/Makefile
  device/test/Makefile
  cmcprov/Makefile
  cmcprov/src/Makefile
  cmcprov/test/Makefile
])
AC_OUTPUT
