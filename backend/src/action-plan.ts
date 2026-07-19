import { z } from "zod";

export const ActionSchema = z.discriminatedUnion("type", [
  z.object({ type: z.literal("CREATE_NOTE"), title: z.string().min(1), content: z.string().min(1) }),
  z.object({ type: z.literal("CREATE_REMINDER"), title: z.string().min(1), scheduled_at: z.string().datetime({ offset: true }) }),
  z.object({ type: z.literal("NO_ACTION"), reason: z.string().min(1) })
]);

export const ActionPlanSchema = z.object({
  summary: z.string().min(1),
  requires_confirmation: z.boolean(),
  actions: z.array(ActionSchema).max(5)
});

export type ActionPlan = z.infer<typeof ActionPlanSchema>;
