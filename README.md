# Keycloak QR Code Authentication 

 [![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

This authentication extension for [Keycloak](https://www.keycloak.org/) provides an authentication execution that allows users to authenticate with another device.

With QR code authentication, users can quickly and securely authenticate without typing passwords or sharing password on untrusted devices. It also enables easier usage of Passkeys on devices without Bluetooth.

## Features

- Provides a standard login method accessible in freemaker templates
- Sign in page auto refreshes to continue to application after remote device approves
- Confirmation of session id (tabID) on confirmation page
- Confirmation of User Agent device, os, and agent on confirmation page

## Compatibility
Compatible with **Keycloak 26.4.2**. Should be compatible with 26.3.0 but has not been tested.

## Installation

1. Download the latest compatible release from the [releases page](https://github.com/HadleySo/keycloak-extension-qr-code-execution/releases)
2. Save the downloaded JAR file into the `providers/` directory inside Keycloak installation folder
3. Stop the Keycloak server
4. Rebuild the installation using `kc.sh build` command
5. Start Keycloak

## Configuration

**No configuration** needed, just add it to your browser flow. 

The [ftl templates](src/main/resources/theme-resources/templates) can be overridden:
- `qr-login-scan.ftl` requires `${url.resourcesPath}/js/qrcode.min.js` and `${url.resourcesPath}/js/jquery.min.js` for javascript, and `QRauthToken` to provide the QR Code URL
- `qr-login-verify.ftl` requires `approveURL` to approve the sign in and `rejectURL` to reject.

## License  

Keycloak QR Code Authentication (keycloak-extension-qr-code-execution / com.hadleyso.keycloak.qrauth) is distributed under [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.txt). Copyright (c) 2025 Hadley So.


[`qrcode.min.js`](https://github.com/davidshimjs/qrcodejs) is distributed under [MIT License](https://mit-license.org/) Copyright (c) 2012 davidshimjs
