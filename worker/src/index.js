// AAd AI personal-use backend on Cloudflare Workers AI.
// Configure AI binding and ACCESS_TOKEN secret in Cloudflare before deploying.
export default {
  async fetch(request, env) {
    if (request.method !== "POST" || new URL(request.url).pathname !== "/chat") {
      return new Response("Not found", { status: 404 });
    }
    if (!env.ACCESS_TOKEN || !env.AI) {
      return Response.json({ error: "Backend not configured" }, { status: 503 });
    }
    const expected = "Bearer " + env.ACCESS_TOKEN;
    if (request.headers.get("Authorization") !== expected) {
      return Response.json({ error: "Unauthorized" }, { status: 401 });
    }
    if (Number(request.headers.get("content-length") || 0) > 8192) {
      return Response.json({ error: "Request too large" }, { status: 413 });
    }
    let body;
    try { body = await request.json(); } catch { return Response.json({ error: "Invalid JSON" }, { status: 400 }); }
    const message = body?.message;
    if (typeof message !== "string" || !message.trim() || message.length > 2000) {
      return Response.json({ error: "Message must be 1-2000 characters" }, { status: 400 });
    }
    try {
      const result = await env.AI.run("@cf/meta/llama-3.1-8b-instruct", {
        messages: [
          { role: "system", content: "You are AAd AI, a friendly helpful assistant. Reply in the language of the user. Support Sorani Kurdish when possible. Do not pretend to know facts you do not know." },
          { role: "user", content: message }
        ],
        max_tokens: 512
      });
      return Response.json({ reply: result.response || "No response." }, {
        headers: { "Cache-Control": "no-store" }
      });
    } catch (error) {
      console.error("AI inference error", error);
      return Response.json({ error: "AI service unavailable or quota exceeded" }, { status: 503 });
    }
  }
};
