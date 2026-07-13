import { useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { completeSprint, createSprint, listSprints, startSprint, type Sprint } from "./sprintBoardApi";
import { ApiError } from "../../lib/apiClient";

/**
 * UI Design S4.5: sprint list + planning/start/complete. The Complete Sprint
 * modal requires an explicit rollover choice before it can be submitted --
 * cannot be dismissed into a default (PRD FR-25).
 */
export function SprintsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [startingSprintId, setStartingSprintId] = useState<string | null>(null);
  const [completingSprintId, setCompletingSprintId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: sprints, isLoading } = useQuery({
    queryKey: ["sprints", projectId],
    queryFn: () => listSprints(projectId!),
    enabled: Boolean(projectId),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["sprints", projectId] });

  const createMutation = useMutation({
    mutationFn: ({ name, goal }: { name: string; goal?: string }) => createSprint(projectId!, name, goal),
    onSuccess: () => {
      invalidate();
      setShowCreateForm(false);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to create sprint."),
  });

  const startMutation = useMutation({
    mutationFn: ({ sprint, startDate, endDate }: { sprint: Sprint; startDate: string; endDate: string }) =>
      startSprint(projectId!, sprint.id, sprint.version, startDate, endDate),
    onSuccess: () => {
      invalidate();
      setStartingSprintId(null);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to start sprint -- is another sprint already active?"),
  });

  const completeMutation = useMutation({
    mutationFn: ({
      sprint,
      rolloverDecision,
      targetSprintId,
    }: {
      sprint: Sprint;
      rolloverDecision: "MOVE_TO_BACKLOG" | "MOVE_TO_NEXT_SPRINT";
      targetSprintId?: string;
    }) => completeSprint(projectId!, sprint.id, sprint.version, rolloverDecision, targetSprintId),
    onSuccess: () => {
      invalidate();
      setCompletingSprintId(null);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to complete sprint."),
  });

  function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    createMutation.mutate({ name: String(form.get("name")), goal: String(form.get("goal") || "") || undefined });
  }

  function handleStart(e: FormEvent<HTMLFormElement>, sprint: Sprint) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    startMutation.mutate({ sprint, startDate: String(form.get("startDate")), endDate: String(form.get("endDate")) });
  }

  function handleComplete(e: FormEvent<HTMLFormElement>, sprint: Sprint) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    const rolloverDecision = form.get("rolloverDecision") as "MOVE_TO_BACKLOG" | "MOVE_TO_NEXT_SPRINT";
    const targetSprintId = String(form.get("targetSprintId") || "") || undefined;
    completeMutation.mutate({ sprint, rolloverDecision, targetSprintId });
  }

  const plannedSprints = sprints?.filter((s) => s.status === "PLANNED" && s.id !== completingSprintId) ?? [];

  return (
    <div>
      <div className="page-header">
        <h2>Sprints</h2>
        <button onClick={() => setShowCreateForm((s) => !s)}>{showCreateForm ? "Cancel" : "Create Sprint"}</button>
      </div>
      {error && <p className="form-error">{error}</p>}

      {showCreateForm && (
        <form onSubmit={handleCreate} className="inline-form">
          <input name="name" placeholder="Sprint name" required />
          <input name="goal" placeholder="Goal (optional)" />
          <button type="submit" disabled={createMutation.isPending}>
            Create
          </button>
        </form>
      )}

      {isLoading ? (
        <p>Loading...</p>
      ) : (
        ["ACTIVE", "PLANNED", "COMPLETED"].map((statusGroup) => (
          <div key={statusGroup} className="sprint-group">
            <h3>{statusGroup}</h3>
            {sprints?.filter((s) => s.status === statusGroup).map((sprint) => (
              <div key={sprint.id} className="sprint-card">
                <strong>{sprint.name}</strong> {sprint.goal && <span> -- {sprint.goal}</span>}
                {sprint.status === "PLANNED" && (
                  <button onClick={() => setStartingSprintId(sprint.id)}>Start</button>
                )}
                {sprint.status === "ACTIVE" && (
                  <button onClick={() => setCompletingSprintId(sprint.id)}>Complete</button>
                )}

                {startingSprintId === sprint.id && (
                  <form onSubmit={(e) => handleStart(e, sprint)} className="inline-form">
                    <label>
                      Start <input name="startDate" type="date" required />
                    </label>
                    <label>
                      End <input name="endDate" type="date" required />
                    </label>
                    <button type="submit" disabled={startMutation.isPending}>
                      Confirm Start
                    </button>
                  </form>
                )}

                {completingSprintId === sprint.id && (
                  <form onSubmit={(e) => handleComplete(e, sprint)} className="rollover-modal">
                    <p>Incomplete issues in this sprint must go somewhere -- choose one:</p>
                    <label>
                      <input type="radio" name="rolloverDecision" value="MOVE_TO_BACKLOG" defaultChecked required /> Move to backlog
                    </label>
                    <label>
                      <input type="radio" name="rolloverDecision" value="MOVE_TO_NEXT_SPRINT" /> Move to another sprint:
                      <select name="targetSprintId">
                        <option value="">Select a planned sprint...</option>
                        {plannedSprints.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button type="submit" disabled={completeMutation.isPending}>
                      Complete Sprint
                    </button>
                  </form>
                )}
              </div>
            ))}
          </div>
        ))
      )}
    </div>
  );
}
