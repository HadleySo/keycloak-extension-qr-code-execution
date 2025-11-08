<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("doQrCodeVerify")}
    <#elseif section = "header">
        ${msg("doQrCodeVerify")}
    <#elseif section = "form">

        <p>${msg("doQrCodeWarning")}<p>

        <p>
            You are authorizing a session on another device <b>${ua_device}</b> running <b>${ua_os}</b> / <b>${ua_agent}</b>.
        </p>

        <p style="padding-top: 15px; padding-bottom: 15px;">Session: <b>${tabId}</b></p>

        <a type="button" href="${approveURL}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">
            ${msg("doAccept")}
        </a>

        <a type="button" href="${rejectURL}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">
            ${msg("doDecline")}
        </a>
        
    </#if>
</@layout.registrationLayout>
