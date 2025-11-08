<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("consentDenied")}
    <#elseif section = "header">
        ${msg("consentDenied")}
    <#elseif section = "form">

        
    </#if>
</@layout.registrationLayout>
