<#macro qrLogin>

    <h3 class="pf-m-l" style="padding-top: 15px">${msg("doQrCodeLogin")}<h3>

    <div id="com-hadleyso-qr-auth-js-target" style="padding-top: 15px; padding-bottom: 15px; width: 45%;" onClick="document.forms['com-hadleyso-qrcode-${QRauthExecId}'].submit();"></div>

    <p style="padding-top: 5px; padding-bottom: 5px; font-size: medium;">Session: ${tabId}</p>

    <form id="com-hadleyso-qrcode-${QRauthExecId}" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <input type="hidden" name="authenticationExecution" value="${QRauthExecId}">
    </form>


       

        
    <#if alignment == "Left">
        <style></style>
    <#elseif alignment == "Center">
        <style>
            #com-hadleyso-qr-auth-js-target img {
                margin-left: auto; 
                margin-right: auto;
            }
        </style>
    <#elseif alignment == "Right">
        <style>
            #com-hadleyso-qr-auth-js-target img {
                margin-left: auto; 
            }
        </style>
    </#if>

    <script src="${url.resourcesPath}/js/jquery.min.js"></script>
    <script src="${url.resourcesPath}/js/qrcode.min.js"></script>
    <script type="text/javascript">
        new QRCode(document.getElementById("com-hadleyso-qr-auth-js-target"), "${QRauthToken}");
    </script>
</#macro>