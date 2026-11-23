# AAd AI — optional Cloudflare Workers AI backend

This backend uses Cloudflare Workers AI's free allocation (subject to Cloudflare's current limits, region, eligibility and model availability). No paid subscription is required *while usage remains within an available free allocation*. Do not enable paid usage if you want zero cost.

## Setup
1. Create a free Cloudflare account at https://dash.cloudflare.com/ .
2. Deploy this worker using Cloudflare Workers / Wrangler or import the GitHub repository and select the worker directory.
3. Ensure the Worker has an AI binding named `AI` (see `wrangler.toml`).
4. Set a strong secret named `ACCESS_TOKEN` in Cloudflare Workers settings (or `npx wrangler secret put ACCESS_TOKEN`). **Never commit this value.**
5. Note your deployed URL, e.g. `https://aad-ai-backend.<your-subdomain>.workers.dev`.
6. In the AAd AI Android app open Settings, enter that HTTPS URL and the same access token.
7. Send a short message.

Do not share the Worker URL together with its access token. A personal token is not robust multi-user authentication; for a public app use real sign-in, rate limiting and abuse prevention. Never put Cloudflare or AI provider API secrets in the Android source.

This is starter code, not yet deployed or verified against a live Cloudflare account. Cloudflare Workers AI models and free quotas can change. Sorani Kurdish quality depends on the model.
