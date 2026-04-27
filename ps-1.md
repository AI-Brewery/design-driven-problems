# Hostel Room Allocation System

## Context

Your college hostel has limited rooms. Students apply, cancel, and swap rooms. Wardens approve requests. Build a system that allocates rooms, manages waitlists, and handles requests fairly.

---

## Knowledge You Need

| Area | Topics |
|---|---|
| OOP | Inheritance, polymorphism, abstract classes, constructors |
| Strings | Comparison, storage, tags/labels |
| Arrays | Sorting, searching, insertion, deletion |
| Algorithms | Sorting by multiple keys, linear search, basic priority logic |
| Language Basics | Classes, lists/arrays, loops, conditionals |

---

## Entities to Model
Student (base)
  → FirstYear
  → Senior
  → ExchangeStudent

Room (base)
  → SingleRoom
  → SharedRoom     // 2 or 3 occupants

Warden
AllocationManager

---

## What the System Must Do

- Accept room applications from students
- Allocate based on priority rules
- Maintain a waitlist when rooms are full
- Handle cancellations → auto-promote next person from waitlist
- Support room swaps between two students

---

## Rules

### Allocation Priority
- Exchange students → always first
- Seniors before juniors
- Within same year → CGPA decides (higher wins)
- Same CGPA → first-come-first-served (timestamp)

### Restrictions
- Shared rooms: all occupants must be same gender
- Some rooms are reserved for specific departments (stored as a string tag)
- A student can hold only one room at a time

---

## API

```
addRoom(roomId, type, capacity, departmentTag, gender)
applyForRoom(studentId, roomId)
cancelAllocation(studentId)
swapRooms(studentId1, studentId2)
getWaitlist(roomId)          → ordered list of studentIds
getRoomOccupants(roomId)     → list of studentIds
```

> Use any OOP language — Java, Python, C++, Kotlin, Dart, etc. The design is what matters.

---

## Open Questions — You Must Define Policies

These are deliberately left open. Your system must handle them. Your write-up must state the policy you chose and why.

1. An exchange student applies but only shared rooms remain — are they forced into a shared room or rejected?
2. Two seniors have identical CGPA and applied at the exact same time. Who wins?
3. A waitlisted student gets offered a room after a cancellation — how long do they have to accept before the next person is tried?

Write your policy. Defend it.

---

## Required Design Doc (graded separately)

For every major data structure or array you use, answer:

- **Why this?** What made it the right fit here?
- **What did you reject?** Name one alternative and why it lost.
- **Cost per operation** — what is the time complexity of: adding a student, cancelling, finding the next waitlisted student?

Then answer this:

> **You must choose one:** optimize for fast allocation OR accurate priority ordering.  
> With arrays only, you likely cannot fully do both.  
> State your choice. Justify it with concrete tradeoffs.

---

## Submission Checklist

- [ ] OOP hierarchy implemented (base + derived classes)
- [ ] All 6 API functions working correctly
- [ ] Waitlist maintained and ordered by priority rules
- [ ] Open policy questions answered in write-up
- [ ] Design doc: structure choices + complexity per operation
- [ ] Tradeoff decision clearly answered

---

## Where Depth Lives

Getting these right requires real thinking — not just writing code:

- **Waitlist ordering** — sort by what? In what order? What if values tie at multiple levels?
- **Swap validation** — before swapping, what must you check? Gender, department tag, occupancy. Order matters.
- **Cascade on cancellation** — when a room opens, who gets it? What if the next waitlisted student is now ineligible (e.g., gender mismatch)?

These cannot be skipped. They are the problem.