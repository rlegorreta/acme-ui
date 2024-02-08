# Important notes for JS upgrade version for Vaadin flow views.

## Vaadin flow .vs. Hilla

The way that Vaadin flow and Hilla support JS files are different.

### Hilla JS files

In Hilla views (e.g., MailView) the JS files are loaded in the `/frontend/generated/jar-resources`
directory. The JS scripts came from the addons, or any library; in this case from 
`third-party-org-vaadin-addon-visjs-network` library. These JS files "in the library" are stored in 
the directory `resources/META-INF/frontend directory`.

### Vaadin flow

For vaadin flow views (e.g., CompanyCompanyView, CompanyPersonView& PersonPersonView) there must be read
using the `page.addJavaScript` statement 

note: do not use `@JSMode`directive because it is loaded immediately (like , like CKEditor) and this does
      not work properly for the `Blockly` library.

So the JS files are stored in `resources/META-INF/frontend/built` directory.

For more information see:
https://vaadin.com/docs/latest/advanced/loading-resources

See the `FlowUI.kt` (if exists) class as an example.

### In case we need to upgrade visjs-network or third-party-org-vaadin-addon-visjs-network library:

Following this approach, when we need to upgrade for some reason en JS in the visjs-network library or in the
`third-party-org-vaadin-addon-visjs-network` JS script code, we need first to generate the JS files int the
project (i.e., Hilla generate gradle task) and after that we need to COPY the generated JS script files
to the acme-ui project directory `resource/META-INF/frontend/built`.
