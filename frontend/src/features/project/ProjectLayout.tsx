import { NavLink, Outlet, useParams } from "react-router-dom";

// UI Design S3: per-project tabs. Board sub-tabs (Scrum/Kanban) are chosen
// inside BoardPage based on the project's methodology, not here.
export function ProjectLayout() {
  const { projectId } = useParams<{ projectId: string }>();

  return (
    <div>
      <nav className="project-subnav">
        <NavLink to={`/projects/${projectId}/backlog`}>Backlog</NavLink>
        <NavLink to={`/projects/${projectId}/board`}>Board</NavLink>
        <NavLink to={`/projects/${projectId}/sprints`}>Sprints</NavLink>
        <NavLink to={`/projects/${projectId}/reports`}>Reports</NavLink>
      </nav>
      <div className="project-content">
        <Outlet />
      </div>
    </div>
  );
}
