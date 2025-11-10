<#macro qrLogin>

    <div id="com-hadleyso-qr-auth">

        <h3 class="pf-m-l" style="padding-top: 15px">${msg("doQrCodeLogin")}<h3>

        <div id="com-hadleyso-qr-auth-js-target" 
            style='padding-top: 15px; padding-bottom: 15px; width: 45%; <#if alignment == "Center">margin-left: auto; margin-right: auto;<#elseif alignment == "Right">margin-left: auto; </#if>' 
            onClick="document.forms['com-hadleyso-qrcode-${QRauthExecId}'].submit();">
        </div>

        <p style="padding-top: 5px; padding-bottom: 5px; font-size: medium;">Session: ${tabId}</p>

        <form id="com-hadleyso-qrcode-${QRauthExecId}" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="authenticationExecution" value="${QRauthExecId}">
        </form>

    </div>

    
    <script src="${url.resourcesPath}/js/jquery.min.js"></script>
    <script src="${url.resourcesPath}/js/qrcode.min.js"></script>
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

        if (getUrlParameter('qr_code_originated') == false) {
            new QRCode(document.getElementById("com-hadleyso-qr-auth-js-target"), "${QRauthToken}");
        } else {
            document.getElementById("com-hadleyso-qr-auth").style.display = "none";
        }
    </script>
</#macro>