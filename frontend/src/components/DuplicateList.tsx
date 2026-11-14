import { Link } from 'react-router-dom'
import type { DuplicateCandidate } from '../api/types'

/**
 * Possible duplicates. Incidents the viewer may not open are shown without title or link: the viewer
 * learns a similar report exists, but not what someone else wrote.
 */
export function DuplicateList({ duplicates, newTab = false }: { duplicates: DuplicateCandidate[]; newTab?: boolean }) {
  return (
    <ul className="duplicate-list">
      {duplicates.map((duplicate) => {
        const score = <em>{Math.round(duplicate.score * 100)}% match</em>
        return (
          <li key={duplicate.reference}>
            {duplicate.accessible && duplicate.incidentId != null ? (
              <Link to={`/incidents/${duplicate.incidentId}`} target={newTab ? '_blank' : undefined}>
                <span>{duplicate.reference}</span>
                <strong>{duplicate.title}</strong>
                {score}
              </Link>
            ) : (
              <div className="duplicate-private">
                <span>{duplicate.reference}</span>
                <strong>A similar incident reported by someone else</strong>
                {score}
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
