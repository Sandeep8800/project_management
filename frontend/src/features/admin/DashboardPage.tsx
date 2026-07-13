import { useQuery } from "@tanstack/react-query";
import { fetchDashboard } from "./adminApi";

// PRD FR-16: consolidated projects/users/role-assignment view.
export function DashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ["admin", "dashboard"], queryFn: fetchDashboard });

  if (isLoading) return <p>Loading...</p>;

  return (
    <div>
      <h2>Admin Dashboard</h2>
      <div className="dashboard-tiles">
        <div className="tile">
          <span className="tile-value">{data?.activeProjectCount}</span>
          <span className="tile-label">Active Projects</span>
        </div>
        <div className="tile">
          <span className="tile-value">{data?.archivedProjectCount}</span>
          <span className="tile-label">Archived Projects</span>
        </div>
        <div className="tile">
          <span className="tile-value">{data?.totalUserCount}</span>
          <span className="tile-label">Total Users</span>
        </div>
      </div>
    </div>
  );
}
