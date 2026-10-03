# prtsys — PRT Employee Management System (SharePoint edition)

Spring Boot + Thymeleaf CRUD app, hosted on Heroku. Employee records are stored in the
University of Dundee **SharePoint Online** list instead of JawsDB MySQL:

> https://dmail.sharepoint.com/sites/MentorMeetingSystem/Lists/employee

The app talks to SharePoint through the **Microsoft Graph REST API**. SharePoint Online is on
the public internet, so **no VPN, Private Space or static IP is needed** on Heroku.

## What changed from the JawsDB version

| Before | After |
|---|---|
| `spring-boot-starter-data-jpa` + `mysql-connector-java` | `spring-boot-starter-security` + `spring-boot-starter-oauth2-client` |
| `EmployeeRepository extends JpaRepository` | `SharePointEmployeeRepository` (Graph REST calls) |
| `@Entity Employee` | Plain `Employee` POJO (`id` = SharePoint item ID) |
| DB credentials in `application.properties` | Azure credentials in Heroku Config Vars |
| Spring Boot 2.5 / Java 8 | Spring Boot 3.5 / Java 17 (`system.properties`) |
| No login | Sign in with University Microsoft account (delegated mode) |

The pages and URLs (`/index`, `/showNewEmployeeForm`, `/api/v1/employeelist`, …) are unchanged.

## 1. Prepare the SharePoint list

Your `employee` list needs columns for first name, last name and email. Create them with
**no spaces** in the name first (e.g. `FirstName`, `LastName`, `Email`), then rename the
display label if you like. The built-in `Title` column is filled with the email by default
(`SHAREPOINT_TITLE_FROM`).

If your columns have other internal names, sign in to the running app and open
`/api/v1/sharepoint/columns`. It lists each column's `name`. Put those values into the
`SHAREPOINT_FIELD_*` config vars.

## 2. Register the app in Microsoft Entra ID (Azure AD)

1. Sign in to https://entra.microsoft.com with `LOchei001@dundee.ac.uk`.
2. **Identity → Applications → App registrations → New registration**
   - Name: `PRT Employee System`
   - Supported account types: *Accounts in this organizational directory only (single tenant)*
   - Redirect URI (Web): `https://<your-heroku-app>.herokuapp.com/login/oauth2/code/azure`
   - Add a second Web redirect URI for local testing: `http://localhost:8500/login/oauth2/code/azure`
3. From **Overview**, copy the **Application (client) ID** and the **Directory (tenant) ID**.
4. **Certificates & secrets → New client secret**. Copy the **Value** straight away (it is only shown once).
5. **API permissions → Add a permission → Microsoft Graph**:
   - **Delegated mode (recommended):** *Delegated permissions* → `Sites.ReadWrite.All`
     (plus `openid`, `profile`, `email`, `offline_access`). Microsoft doesn't normally require admin
     consent for this delegated permission, but Dundee may block user consent. If sign-in shows
     "Need admin approval", ask DTS to grant consent.
   - **App mode:** *Application permissions* → `Sites.Selected` (preferred) or `Sites.ReadWrite.All`.
     These always need **admin consent from DTS**. With `Sites.Selected`, DTS must also grant the
     app `write` access to the `MentorMeetingSystem` site.

If **New registration** is greyed out, ask DTS (Help4U service request) to create the
registration for you and send you the client ID, tenant ID and secret.

### Delegated vs app mode

- **delegated** (default): every user signs in with their Dundee account. The app reads and
  writes SharePoint *as that user*, so SharePoint permissions on the list decide who can do what.
  Nobody outside the University can use the app.
- **app**: the app uses its own identity, and pages are open to anyone with the URL
  (like the original app). Only use this if you add your own access control.

## 3. Configure Heroku

Remove the old database (and its `JAWSDB_URL` var) once you've migrated any data you need:

```bash
heroku addons:destroy jawsdb --app <your-heroku-app>      # optional
```

Set the config vars (Dashboard → Settings → Reveal Config Vars, or CLI):

```bash
heroku config:set --app <your-heroku-app> \
  SHAREPOINT_AUTH_MODE=delegated \
  AZURE_TENANT_ID=<directory-tenant-id-or-dundee.ac.uk> \
  AZURE_CLIENT_ID=<application-client-id> \
  AZURE_CLIENT_SECRET=<client-secret-value> \
  SHAREPOINT_SITE_URL=https://dmail.sharepoint.com/sites/MentorMeetingSystem \
  SHAREPOINT_LIST=employee
```

Optional overrides:

| Variable | Default | Meaning |
|---|---|---|
| `SHAREPOINT_FIELD_FIRST_NAME` | `FirstName` | internal column name |
| `SHAREPOINT_FIELD_LAST_NAME` | `LastName` | internal column name |
| `SHAREPOINT_FIELD_EMAIL` | `Email` | internal column name |
| `SHAREPOINT_TITLE_FROM` | `email` | value for `Title`: `email`, `firstName`, `lastName`, `fullName`, `none` |
| `SHAREPOINT_SITE_ID` | (looked up) | Graph site id, to skip the lookup |

## 4. Push to GitHub and deploy

```bash
cd prtapp-master
git init
git add .
git update-index --chmod=+x mvnw         # Heroku needs mvnw to be executable
git commit -m "Store employees in SharePoint Online via Microsoft Graph"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

On Heroku: **Deploy → Deployment method → GitHub**, pick the repo, then **Deploy Branch**
(or enable Automatic Deploys).

## 5. Check it works

1. Open `https://<your-heroku-app>.herokuapp.com/` and sign in with your Dundee account.
2. `/api/v1/sharepoint/status` shows the resolved site and list IDs.
3. `/api/v1/sharepoint/columns` lists the list's columns.
4. Add an employee, then confirm the new row appears in the SharePoint list.

## Run locally

```bash
cp .env.example .env      # fill in the values
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run    # http://localhost:8500
```

## Security notes

- The old `application.properties` and `bitbucket-pipelines.yml` contained database passwords and a
  **Heroku API key** in plain text. They are removed here, but they remain in the old repository's
  history. **Regenerate the Heroku API key** (Account settings → API Key) and destroy the old
  databases.
- Keep `AZURE_CLIENT_SECRET` only in Heroku Config Vars. Secrets expire, so note the expiry date.
