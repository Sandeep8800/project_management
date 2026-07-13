import { useQuery } from "@tanstack/react-query";
import { fetchMyProjects } from "../auth/authApi";

// UI Design S3 project switcher, backed by GET /me/projects (API Design S4, the
// gap folded back in after UI Design surfaced it). Project detail screens
// (Backlog/Board/Sprints/Reports) are follow-up work -- their backend modules
// are stubbed in this pass, so this list is a landing point, not yet a link
// into working project screens.
export function HomePage() {
  const { data: projects, isLoading } = useQuery({
    queryKey: ["me", "projects"],
    queryFn: fetchMyProjects,
  });

  if (isLoading) return <p>Loading your projects...</p>;

  if (!projects || projects.length === 0) {
    return <p>You have not been assigned to any projects yet. Contact your administrator.</p>;
  }

  return (
    <div>
      <h2>Your Projects</h2>
      <ul className="project-list">
        {projects.map((p) => (
          <li key={p.projectId}>
            <strong>{p.projectKey}</strong> — {p.projectName}
            <span className="role-badge">{p.role}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
