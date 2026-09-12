import { Injectable, signal } from '@angular/core';

const ACTOR_NAME_KEY = 'rrd_actor_name';

/**
 * The name recorded as approvedBy / assignedTo when a human takes a HITL
 * action (approve an order, acknowledge/resolve an escalation). Kept in one
 * place and shown once in the nav bar rather than re-prompted on every
 * action — set it once per session.
 */
@Injectable({ providedIn: 'root' })
export class ActorNameService {
    readonly actorName = signal(sessionStorage.getItem(ACTOR_NAME_KEY) ?? '');

    setActorName(name: string): void {
        this.actorName.set(name);
        sessionStorage.setItem(ACTOR_NAME_KEY, name);
    }
}