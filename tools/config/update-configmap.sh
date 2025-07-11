envValue=$1
APP_NAME=$2
PEN_NAMESPACE=$3
COMMON_NAMESPACE=$4
DB_JDBC_CONNECT_STRING=$5
DB_PWD=$6
DB_USER=$7
SPLUNK_TOKEN=$8
EDX_NAMESPACE=$9

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca

SOAM_KC_LOAD_USER_ADMIN=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

NATS_CLUSTER=educ_nats_cluster
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Retrieving client ID for pen-nominal-roll-api-service
PNR_CLIENT_ID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="pen-nominal-roll-api-service")' | jq -r '.id')

echo
echo Removing PEN NOMINAL ROLL API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$PNR_CLIENT_ID" \
  -H "Authorization: Bearer $TKN"

echo
echo Creating client pen-nominal-roll-api-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"pen-nominal-roll-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\",   \"role_list\", \"profile\", \"roles\",  \"READ_FED_PROV_CODE\",\"READ_STUDENT_CODES\",\"READ_SDC_SCHOOL_COLLECTION_STUDENT\",\"READ_SDC_COLLECTION\", \"DELETE_FED_PROV_CODE\", \"READ_SCHOOL\",\"READ_DISTRICT\",\"READ_INDEPENDENT_AUTHORITY\", \"WRITE_FED_PROV_CODE\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo
echo Retrieving client ID for pen-nominal-roll-api-service
PNR_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="pen-nominal-roll-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for pen-nominal-roll-api-service
PNR_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$PNR_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Writing scope NOMINAL_ROLL_READ_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read nominal roll student\",\"id\": \"NOMINAL_ROLL_READ_STUDENT\",\"name\": \"NOMINAL_ROLL_READ_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_WRITE_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write nominal roll student\",\"id\": \"NOMINAL_ROLL_WRITE_STUDENT\",\"name\": \"NOMINAL_ROLL_WRITE_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_DELETE_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Delete nominal roll student\",\"id\": \"NOMINAL_ROLL_DELETE_STUDENT\",\"name\": \"NOMINAL_ROLL_DELETE_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_UPLOAD_FILE
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Upload nominal roll file\",\"id\": \"NOMINAL_ROLL_UPLOAD_FILE\",\"name\": \"NOMINAL_ROLL_UPLOAD_FILE\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_VALIDATE
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Validate nominal roll student\",\"id\": \"NOMINAL_ROLL_VALIDATE\",\"name\": \"NOMINAL_ROLL_VALIDATE\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_POST_DATA_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Start post data saga\",\"id\": \"NOMINAL_ROLL_POST_DATA_SAGA\",\"name\": \"NOMINAL_ROLL_POST_DATA_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_READ_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Fetch nominal roll saga information\",\"id\": \"NOMINAL_ROLL_READ_SAGA\",\"name\": \"NOMINAL_ROLL_READ_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_WRITE_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write nominal roll saga information\",\"id\": \"NOMINAL_ROLL_WRITE_SAGA\",\"name\": \"NOMINAL_ROLL_WRITE_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope NOMINAL_ROLL_CREATE_FED_PROV
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Create nominal roll fed prov code\",\"id\": \"NOMINAL_ROLL_CREATE_FED_PROV\",\"name\": \"NOMINAL_ROLL_CREATE_FED_PROV\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"


###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
   Buffer_Chunk_Size 5MB
   Buffer_Max_Size 5MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0/30 * * * * *"
SCHEDULED_JOBS_LOAD_SCHOOL_CRON="0 0 0/12 * * *"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="25s"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="25s"
SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON="0/2 * * * * *"
SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_MOST_FOR="1900ms"
SCHEDULED_JOBS_PURGE_OLD_EVENT_RECORDS_CRON="0 0 0 * * *"
PURGE_RECORDS_SAGA_AFTER_DAYS=365

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=ORACLE_USERNAME="$DB_USER" --from-literal=ORACLE_PASSWORD="$DB_PWD" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=NATS_CLUSTER="$NATS_CLUSTER" --from-literal=SPRING_JPA_SHOW_SQL="false"  --from-literal=CLIENT_ID="pen-nominal-roll-api-service" --from-literal=CLIENT_SECRET="$PNR_APIServiceClientSecret"  --from-literal=INSTITUTE_API_URL="http://institute-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/institute" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=NOM_ROLL_FIELD_INVALID_THRESHOLD=100 --from-literal=NATS_URL="$NATS_URL" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=SDC_API_URL="http://student-data-collection-api-master.$EDX_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student-data-collection" --from-literal=STUDENT_API_URL="http://student-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=SCHEDULED_JOBS_LOAD_SCHOOL_CRON="$SCHEDULED_JOBS_LOAD_SCHOOL_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON="$SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_NOM_ROLL_STUDENTS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PURGE_OLD_EVENT_RECORDS_CRON="$SCHEDULED_JOBS_PURGE_OLD_EVENT_RECORDS_CRON" --from-literal=PURGE_RECORDS_SAGA_AFTER_DAYS="$PURGE_RECORDS_SAGA_AFTER_DAYS" --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-main application
oc -n "$PEN_NAMESPACE-$envValue" set env --from=configmap/"$APP_NAME"-config-map deployment/"$APP_NAME"-main

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -

echo Removing un-needed config entries
oc -n "$PEN_NAMESPACE"-"$envValue" set env deployment/"$APP_NAME"-main KEYCLOAK_PUBLIC_KEY-
