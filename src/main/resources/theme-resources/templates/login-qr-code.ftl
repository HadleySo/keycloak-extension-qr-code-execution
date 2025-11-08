<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("doQrCodeLogin")}
    <#elseif section = "header">
        ${msg("doQrCodeLogin")}
    <#elseif section = "form">

        <p>${QRauthToken}</p>

        <form id="com-hadleyso-qrcode-${QRauthExecId}" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="authenticationExecution" value="${QRauthExecId}">
            <input type="submit" value="${msg("doLogIn")}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"/>
        </form>

        <#--  <script>
            // Wait 15 seconds 
            setTimeout(function() {
                document.getElementById("com-hadleyso-qrcode-${QRauthExecId}").submit();
            }, 15000);
        </script>  -->
    </#if>
</@layout.registrationLayout>
