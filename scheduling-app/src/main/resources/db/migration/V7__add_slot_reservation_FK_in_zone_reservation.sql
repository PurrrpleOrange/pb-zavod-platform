set local search_path to scheduling;

alter table zone_reservation
    add column if not exists slot_reservation_id uuid;

do $$
begin
    if not exists (
        select 1
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        join pg_namespace n on n.oid = t.relnamespace
        where c.conname = 'fk_zone_reservation_slot_reservation'
          and n.nspname = 'scheduling'
    ) then
alter table zone_reservation
    add constraint fk_zone_reservation_slot_reservation
        foreign key (slot_reservation_id)
            references slot_reservation(slot_reservation_id);
end if;
end $$;

create index if not exists ix_zone_reservation_slot_reservation_id
    on zone_reservation(slot_reservation_id);