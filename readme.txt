+======================================================+
| Before proceeding ensure that project is FULLY BUILT |
| using the build tool you chose.                      |
+======================================================+

Start Server:
-------------
To start the REST server run the following:
  $ bin/nlpcraft.{sh|cmd} start-server
NOTE: you can execute 'start-server' command when running bin/nlpcraft.{sh|cmd} script in REPL mode.

You can also start the REST server programmatically (with default configuration) by
starting the following JVM run configuration:
  Main class: org.apache.nlpcraft.NCStart
  Program arguments: -server

Start Data Probe:
-----------------
To start a data probe with the default 'src/main/resources/probe.conf' configuration
start the following JVM run configuration:
  Main class: org.apache.nlpcraft.NCStart
  Program arguments: -probe

Start Data Probe (2a):
---------------------
You can also start data probe and override any property from the default
'src/main/resources/probe.conf' file. For example:
  Main class: org.apache.nlpcraft.NCStart
  Program arguments: -probe
  VM arguments: -Dconfig.override_with_env_vars=true
  Environment variables: CONFIG_FORCE_nlpcraft_probe_models=demo.Weather

Start Data Probe (2b):
---------------------
You can also start data probe programmatically using embedded probe:
  NCEmbeddedProbe.start(Weather.class);

Testing Model:
--------------
Once REST server and data probe started you can access and test your model. You can test your
model using any REST tools. The easiest way is to use built-in bin/nlpcraft.{sh|cmd} script in
REPL (default) mode:
  # Start script without parameter to enter REPL interactive mode.
    $ bin/nlpcraft.{sh|cmd}
  # Ensure that server is started. Make sure the data probe is started as well and connected to the server.
    > info-server
  # Issue '/ask/sync' REST call for the model.
    > ask --txt="word1" --mdlId=Weather
  # Issue '/ask/sync' REST call for the model.
    > ask --txt="some word1" --mdlId=Weather
  # Issue '/ask/sync' REST call for the model.
    > ask --txt="some word2" --mdlId=Weather

Model Auto-Validator:
---------------------
To run auto-validator (built-in model unit test) for this model run the following JVM run configuration:
  Main class: org.apache.nlpcraft.model.tools.test.NCTestAutoModelValidator
  VM arguments: -DNLPCRAFT_TEST_MODELS=demo.Weather

Note that auto-validator will AUTOMATICALLY start an embedded data probe for this model so there
is NO NEED to start a data probe separately when running an auto-validator.