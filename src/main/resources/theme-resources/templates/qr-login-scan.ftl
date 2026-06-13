<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("doQrCodeLogin")}
    <#elseif section = "header">
        ${msg("doQrCodeLogin")}
    <#elseif section = "form">

        <div id="com-hadleyso-qr-auth-js-target" 
        style='padding-top: 15px; padding-bottom: 15px; width: 45%; <#if alignment == "Center">margin-left: auto; margin-right: auto;<#elseif alignment == "Right">margin-left: auto; </#if>' 
        onClick="document.forms['com-hadleyso-qrcode-${QRauthExecId}'].submit();">
            <span style="display: none;">${QRauthToken}</span>
            <img id="com-hadleyso-qr-auth-qr-code" src="data:image/png;base64,${QRauthImage}" alt="Figure: Barcode">
        </div>

        <p style="padding-top: 15px; padding-bottom: 15px;">
        <b>Session: </b>${tabId}
        </p>
        <#if ShortCode != "">
        <p style="padding-bottom: 15px;" id="com-hadleyso-qr-auth-short-zone">
        <a href="#" id="com-hadleyso-qr-auth-short-start">${msg("CannotScan")}</a> | <a href="${ShortCodeLink}">${msg("UseShortCode")}</a>
        </p>
        </#if>


        <#if ShortCode != "">
        <p style="padding-top: 15px; padding-bottom: 15px; display: none;" id="com-hadleyso-qr-auth-short-message">
        ${msg("doShortCodeSteps")}
        <br><br>
        <b>Short Code:</b> ${ShortCode}
        </p>
        </#if>

        <form id="com-hadleyso-qrcode-${QRauthExecId}" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="authenticationExecution" value="${QRauthExecId}">
            <input type="submit" value="${msg('doLogIn')}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"/>
        </form>

        <#if refreshRate != 0>
            <script>
                // Wait 15 seconds 
                setTimeout(function() {
                    document.getElementById("com-hadleyso-qrcode-${QRauthExecId}").submit();
                }, ${refreshRate}000);
            </script>
        </#if>

        <script type="text/javascript">
            var getUrlParameter = function getUrlParameter(sParam) {
                var sPageURL = window.location.search.substring(1),
                    sURLVariables = sPageURL.split('&'),
                    sParameterName,
                    i;

                for (i = 0; i < sURLVariables.length; i++) {
                    sParameterName = sURLVariables[i].split('=');

                    if (sParameterName[0] === sParam) {
                        return true;
                    }
                }
                return false;
            };

            if (getUrlParameter('qr_code_originated') == true) {
                document.getElementById("com-hadleyso-qr-auth").style.display = "none";
            }
        </script>
        <#if ShortCode != "">
            <script type="text/javascript">
                document.getElementById("com-hadleyso-qr-auth-short-start").addEventListener("click", function () {
                    document.getElementById("com-hadleyso-qr-auth-js-target").style.display = "none";
                    document.getElementById("com-hadleyso-qr-auth-short-zone").style.display = "none";
                    document.getElementById("com-hadleyso-qr-auth-short-message").style.display = "block";
                });
            </script>
        </#if>
    </#if>
</@layout.registrationLayout>
