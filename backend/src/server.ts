import Fastify from "fastify";
import OpenAI from "openai";
import { z } from "zod";
import { ActionPlanSchema } from "./action-plan.js";

const app = Fastify({ logger: true });
const port = Number(process.env.PORT ?? 8080);
const model = process.env.OPENAI_MODEL ?? "gpt-5.6-terra";

const PlanRequestSchema = z.object({
  transcript: z.string().min(1).max(4000),
  timezone: z.string().default("Europe/Berlin"),
  current_time: z.string().datetime({ offset: true }).optional()
});

app.get("/health", async () => ({ status: "ok" }));

app.post("/agent/plan", async (request, reply) => {
  const parsed = PlanRequestSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: "invalid_request", details: parsed.error.flatten() });
  }
  if (!process.env.OPENAI_API_KEY) {
    return reply.code(503).send({ error: "openai_not_configured" });
  }

  const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
  const response = await client.responses.create({
    model,
    input: [
      {
        role: "system",
        content:
          "Convert the driver request into a short, safe action plan. Never invent missing targets or times. " +
          "Use NO_ACTION when uncertain. Every CREATE_NOTE or CREATE_REMINDER requires confirmation."
      },
      { role: "user", content: JSON.stringify(parsed.data) }
    ],
    text: {
      format: {
        type: "json_schema",
        name: "action_plan",
        strict: true,
        schema: {
          type: "object",
          additionalProperties: false,
          required: ["summary", "requires_confirmation", "actions"],
          properties: {
            summary: { type: "string" },
            requires_confirmation: { type: "boolean" },
            actions: {
              type: "array",
              maxItems: 5,
              items: {
                oneOf: [
                  {
                    type: "object",
                    additionalProperties: false,
                    required: ["type", "title", "content"],
                    properties: {
                      type: { const: "CREATE_NOTE" },
                      title: { type: "string" },
                      content: { type: "string" }
                    }
                  },
                  {
                    type: "object",
                    additionalProperties: false,
                    required: ["type", "title", "scheduled_at"],
                    properties: {
                      type: { const: "CREATE_REMINDER" },
                      title: { type: "string" },
                      scheduled_at: { type: "string" }
                    }
                  },
                  {
                    type: "object",
                    additionalProperties: false,
                    required: ["type", "reason"],
                    properties: {
                      type: { const: "NO_ACTION" },
                      reason: { type: "string" }
                    }
                  }
                ]
              }
            }
          }
        }
      }
    }
  });

  return reply.send(ActionPlanSchema.parse(JSON.parse(response.output_text)));
});

app.setErrorHandler((error, _request, reply) => {
  app.log.error(error);
  reply.code(500).send({ error: "internal_error" });
});

await app.listen({ host: "0.0.0.0", port });
