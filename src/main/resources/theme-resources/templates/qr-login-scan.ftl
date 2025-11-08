<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("doQrCodeLogin")}
    <#elseif section = "header">
        ${msg("doQrCodeLogin")}
    <#elseif section = "form">

        <div id="com-hadleyso-qr-auth-js-target" style="padding-top: 15px; padding-bottom: 15px;"></div>

        <p style="padding-top: 15px; padding-bottom: 15px;"><b>Session: </b>${tabId}</p>

        <form id="com-hadleyso-qrcode-${QRauthExecId}" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="authenticationExecution" value="${QRauthExecId}">
            <input type="submit" value="${msg("doLogIn")}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"/>
        </form>

        <script>
            // Wait 15 seconds 
            setTimeout(function() {
                document.getElementById("com-hadleyso-qrcode-${QRauthExecId}").submit();
            }, 15000);
        </script>

        <script src="${url.resourcesPath}/js/jquery.min.js"></script>
        <script src="${url.resourcesPath}/js/qrcode.min.js"></script>
        <script type="text/javascript">
            new QRCode(document.getElementById("com-hadleyso-qr-auth-js-target"), "${QRauthToken}");
        </script>
    </#if>
</@layout.registrationLayout>
