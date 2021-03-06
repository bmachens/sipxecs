create table branch_branch
(
  branch_id integer not null,
  associated_branch_id integer not null,
  constraint branch_branch_pkey primary key (branch_id, associated_branch_id),
  constraint branch_branch_fk1 foreign key (branch_id)
      references branch (branch_id) match simple
      on update no action on delete no action,
  constraint branch_branch_fk2 foreign key (associated_branch_id)
      references branch (branch_id) match simple
      on update no action on delete no action
);