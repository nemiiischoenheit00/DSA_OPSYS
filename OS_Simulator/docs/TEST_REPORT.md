# Test report

Generated from an actual run on 2026-11-16 with Python 3.12.3 (Linux sandbox).

## Command
```
python -m unittest discover -s tests -t . -v
```

## Result
```
----------------------------------------------------------------------
Ran 103 tests in 1.568s

OK
```

## Full list of executed tests
```
test_allocation_exceeds_maximum (tests.test_bankers.TestBankers.test_allocation_exceeds_maximum) ... ok
test_boundary_single_process_single_resource (tests.test_bankers.TestBankers.test_boundary_single_process_single_resource) ... ok
test_large_random_safe_state (tests.test_bankers.TestBankers.test_large_random_safe_state) ... ok
test_large_unsafe_state (tests.test_bankers.TestBankers.test_large_unsafe_state) ... ok
test_need_matrix (tests.test_bankers.TestBankers.test_need_matrix) ... ok
test_negative_values_and_shape_errors (tests.test_bankers.TestBankers.test_negative_values_and_shape_errors) ... ok
test_partially_runnable_but_unsafe (tests.test_bankers.TestBankers.test_partially_runnable_but_unsafe) ... ok
test_request_denied_unsafe (tests.test_bankers.TestBankers.test_request_denied_unsafe) ... ok
test_request_exceeds_need_or_available (tests.test_bankers.TestBankers.test_request_exceeds_need_or_available) ... ok
test_request_granted (tests.test_bankers.TestBankers.test_request_granted) ... ok
test_safe_state_and_sequence (tests.test_bankers.TestBankers.test_safe_state_and_sequence) ... ok
test_textbook_follow_up_requests (tests.test_bankers.TestBankers.test_textbook_follow_up_requests) ... ok
test_unsafe_state (tests.test_bankers.TestBankers.test_unsafe_state) ... ok
test_zero_need_everywhere (tests.test_bankers.TestBankers.test_zero_need_everywhere) ... ok
test_fcfs_textbook (tests.test_cpu_scheduling.TestCPUScheduling.test_fcfs_textbook) ... ok
test_idle_in_round_robin (tests.test_cpu_scheduling.TestCPUScheduling.test_idle_in_round_robin) ... ok
test_idle_time_at_start_and_middle (tests.test_cpu_scheduling.TestCPUScheduling.test_idle_time_at_start_and_middle) ... ok
test_invalid_inputs (tests.test_cpu_scheduling.TestCPUScheduling.test_invalid_inputs) ... ok
test_large_dataset_invariants (tests.test_cpu_scheduling.TestCPUScheduling.test_large_dataset_invariants) ... ok
test_priority_higher_number_is_higher (tests.test_cpu_scheduling.TestCPUScheduling.test_priority_higher_number_is_higher) ... ok
test_priority_non_preemptive (tests.test_cpu_scheduling.TestCPUScheduling.test_priority_non_preemptive) ... ok
test_priority_preemptive (tests.test_cpu_scheduling.TestCPUScheduling.test_priority_preemptive) ... ok
test_quantum_larger_than_bursts_equals_fcfs (tests.test_cpu_scheduling.TestCPUScheduling.test_quantum_larger_than_bursts_equals_fcfs) ... ok
test_response_time (tests.test_cpu_scheduling.TestCPUScheduling.test_response_time) ... ok
test_round_robin_arrival_before_requeue (tests.test_cpu_scheduling.TestCPUScheduling.test_round_robin_arrival_before_requeue) ... ok
test_round_robin_textbook (tests.test_cpu_scheduling.TestCPUScheduling.test_round_robin_textbook) ... ok
test_single_process (tests.test_cpu_scheduling.TestCPUScheduling.test_single_process) ... ok
test_sjf_non_preemptive (tests.test_cpu_scheduling.TestCPUScheduling.test_sjf_non_preemptive) ... ok
test_srtf_never_worse_than_fcfs_average_waiting (tests.test_cpu_scheduling.TestCPUScheduling.test_srtf_never_worse_than_fcfs_average_waiting) ... ok
test_srtf_preemptive (tests.test_cpu_scheduling.TestCPUScheduling.test_srtf_preemptive) ... ok
test_ties_broken_by_arrival_then_entry_order (tests.test_cpu_scheduling.TestCPUScheduling.test_ties_broken_by_arrival_then_entry_order) ... ok
test_clook_up_and_down (tests.test_disk_scheduling.TestDiskScheduling.test_clook_up_and_down) ... ok
test_cscan_up_and_down (tests.test_disk_scheduling.TestDiskScheduling.test_cscan_up_and_down) ... ok
test_custom_cylinder_limits (tests.test_disk_scheduling.TestDiskScheduling.test_custom_cylinder_limits) ... ok
test_duplicates_and_single_request (tests.test_disk_scheduling.TestDiskScheduling.test_duplicates_and_single_request) ... ok
test_fcfs (tests.test_disk_scheduling.TestDiskScheduling.test_fcfs) ... ok
test_head_at_edge (tests.test_disk_scheduling.TestDiskScheduling.test_head_at_edge) ... ok
test_invalid_input (tests.test_disk_scheduling.TestDiskScheduling.test_invalid_input) ... ok
test_jump_can_be_excluded (tests.test_disk_scheduling.TestDiskScheduling.test_jump_can_be_excluded) ... ok
test_large_dataset_invariants (tests.test_disk_scheduling.TestDiskScheduling.test_large_dataset_invariants) ... ok
test_look_up_and_down (tests.test_disk_scheduling.TestDiskScheduling.test_look_up_and_down) ... ok
test_registry_is_modular (tests.test_disk_scheduling.TestDiskScheduling.test_registry_is_modular) ... ok
test_request_equal_to_head_costs_nothing (tests.test_disk_scheduling.TestDiskScheduling.test_request_equal_to_head_costs_nothing) ... ok
test_requests_on_boundaries_not_double_counted (tests.test_disk_scheduling.TestDiskScheduling.test_requests_on_boundaries_not_double_counted) ... ok
test_scan_down (tests.test_disk_scheduling.TestDiskScheduling.test_scan_down) ... ok
test_scan_up_goes_to_edge (tests.test_disk_scheduling.TestDiskScheduling.test_scan_up_goes_to_edge) ... ok
test_sstf (tests.test_disk_scheduling.TestDiskScheduling.test_sstf) ... ok
test_exact_fit_and_too_big (tests.test_fragmentation.TestFragmentation.test_exact_fit_and_too_big) ... ok
test_fixed_best_fit_places_everything (tests.test_fragmentation.TestFragmentation.test_fixed_best_fit_places_everything) ... ok
test_fixed_external_fragmentation_detected (tests.test_fragmentation.TestFragmentation.test_fixed_external_fragmentation_detected) ... ok
test_fixed_first_fit (tests.test_fragmentation.TestFragmentation.test_fixed_first_fit) ... ok
test_fixed_internal_vs_external_not_confused (tests.test_fragmentation.TestFragmentation.test_fixed_internal_vs_external_not_confused) ... ok
test_fixed_worst_fit (tests.test_fragmentation.TestFragmentation.test_fixed_worst_fit) ... ok
test_invalid_input (tests.test_fragmentation.TestFragmentation.test_invalid_input) ... ok
test_large_dataset_invariants (tests.test_fragmentation.TestFragmentation.test_large_dataset_invariants) ... ok
test_variable_best_fit (tests.test_fragmentation.TestFragmentation.test_variable_best_fit) ... ok
test_variable_block_holds_several_processes (tests.test_fragmentation.TestFragmentation.test_variable_block_holds_several_processes) ... ok
test_variable_first_fit_external_fragmentation (tests.test_fragmentation.TestFragmentation.test_variable_first_fit_external_fragmentation) ... ok
test_bankers_dynamic_size_and_validation (tests.test_gui_smoke.TestGuiSmoke.test_bankers_dynamic_size_and_validation) ... ok
test_cpu_all_algorithms_and_bad_values (tests.test_gui_smoke.TestGuiSmoke.test_cpu_all_algorithms_and_bad_values) ... ok
test_cpu_large_dataset_via_random_generator (tests.test_gui_smoke.TestGuiSmoke.test_cpu_large_dataset_via_random_generator) ... ok
test_disk_all_algorithms_and_invalid (tests.test_gui_smoke.TestGuiSmoke.test_disk_all_algorithms_and_invalid) ... ok
test_empty_default_inputs_are_handled (tests.test_gui_smoke.TestGuiSmoke.test_empty_default_inputs_are_handled) ... ok
test_every_module_example_runs (tests.test_gui_smoke.TestGuiSmoke.test_every_module_example_runs) ... ok
test_fragmentation_modes_and_multiprogramming (tests.test_gui_smoke.TestGuiSmoke.test_fragmentation_modes_and_multiprogramming) ... ok
test_page_replacement_variants_and_invalid (tests.test_gui_smoke.TestGuiSmoke.test_page_replacement_variants_and_invalid) ... ok
test_paging_invalid_values (tests.test_gui_smoke.TestGuiSmoke.test_paging_invalid_values) ... ok
test_segmentation_errors (tests.test_gui_smoke.TestGuiSmoke.test_segmentation_errors) ... ok
test_contiguous_memory_external_fragmentation (tests.test_multiprogramming.TestMultiprogramming.test_contiguous_memory_external_fragmentation) ... ok
test_everything_rejected (tests.test_multiprogramming.TestMultiprogramming.test_everything_rejected) ... ok
test_hand_calculated_example (tests.test_multiprogramming.TestMultiprogramming.test_hand_calculated_example) ... ok
test_idle_cpu_waiting_for_arrival (tests.test_multiprogramming.TestMultiprogramming.test_idle_cpu_waiting_for_arrival) ... ok
test_invalid_input (tests.test_multiprogramming.TestMultiprogramming.test_invalid_input) ... ok
test_job_too_large_is_rejected (tests.test_multiprogramming.TestMultiprogramming.test_job_too_large_is_rejected) ... ok
test_large_dataset_invariants (tests.test_multiprogramming.TestMultiprogramming.test_large_dataset_invariants) ... ok
test_os_reserved_memory_and_addresses (tests.test_multiprogramming.TestMultiprogramming.test_os_reserved_memory_and_addresses) ... ok
test_snapshots_show_loaded_and_waiting (tests.test_multiprogramming.TestMultiprogramming.test_snapshots_show_loaded_and_waiting) ... ok
test_strict_fcfs_versus_bypass (tests.test_multiprogramming.TestMultiprogramming.test_strict_fcfs_versus_bypass) ... ok
test_belady_anomaly_for_fifo (tests.test_page_replacement.TestPageReplacement.test_belady_anomaly_for_fifo) ... ok
test_boundaries (tests.test_page_replacement.TestPageReplacement.test_boundaries) ... ok
test_fifo_frame_states (tests.test_page_replacement.TestPageReplacement.test_fifo_frame_states) ... ok
test_hits_ratios (tests.test_page_replacement.TestPageReplacement.test_hits_ratios) ... ok
test_invalid_input (tests.test_page_replacement.TestPageReplacement.test_invalid_input) ... ok
test_large_reference_string_invariants (tests.test_page_replacement.TestPageReplacement.test_large_reference_string_invariants) ... ok
test_lru_evicts_least_recently_used (tests.test_page_replacement.TestPageReplacement.test_lru_evicts_least_recently_used) ... ok
test_optimal_evicts_farthest_next_use (tests.test_page_replacement.TestPageReplacement.test_optimal_evicts_farthest_next_use) ... ok
test_textbook_fault_counts (tests.test_page_replacement.TestPageReplacement.test_textbook_fault_counts) ... ok
test_basic_translation (tests.test_paging.TestPaging.test_basic_translation) ... ok
test_frame_table_and_auto_map (tests.test_paging.TestPaging.test_frame_table_and_auto_map) ... ok
test_invalid_addresses (tests.test_paging.TestPaging.test_invalid_addresses) ... ok
test_invalid_configuration_and_table (tests.test_paging.TestPaging.test_invalid_configuration_and_table) ... ok
test_large_configuration (tests.test_paging.TestPaging.test_large_configuration) ... ok
test_page_boundaries (tests.test_paging.TestPaging.test_page_boundaries) ... ok
test_page_size_one_and_non_power_of_two (tests.test_paging.TestPaging.test_page_size_one_and_non_power_of_two) ... ok
test_unmapped_page_is_page_fault (tests.test_paging.TestPaging.test_unmapped_page_is_page_fault) ... ok
test_invalid_tables (tests.test_segmentation.TestSegmentation.test_invalid_tables) ... ok
test_large_table (tests.test_segmentation.TestSegmentation.test_large_table) ... ok
test_no_overlap_warnings_for_textbook_table (tests.test_segmentation.TestSegmentation.test_no_overlap_warnings_for_textbook_table) ... ok
test_offset_boundary (tests.test_segmentation.TestSegmentation.test_offset_boundary) ... ok
test_out_of_range_and_bad_segment (tests.test_segmentation.TestSegmentation.test_out_of_range_and_bad_segment) ... ok
test_overlap_warning (tests.test_segmentation.TestSegmentation.test_overlap_warning) ... ok
test_physical_memory_check (tests.test_segmentation.TestSegmentation.test_physical_memory_check) ... ok
test_valid_translations (tests.test_segmentation.TestSegmentation.test_valid_translations) ... ok
```

## What the tests verify
| Module | Verified against | Small / boundary / invalid / large |
|---|---|---|
| CPU | hand-calculated textbook schedules (FCFS 17/27, RR q=4 waits 6,4,7; SJF 4.0; SRTF 3.0; priority 8.2), idle time, ties, RR arrival order | yes / yes / yes / 2000 processes (work conservation, gap-free Gantt) |
| Banker's | classic 5x3 example (Need, sequence P1,P3,P0,P2,P4, textbook request follow-ups), independent sequence checker | yes / yes / yes / 300x12 safe, 200 unsafe |
| Paging | hand-computed translations, page boundaries, faults, invalid addresses | yes / yes / yes / 10000 pages |
| Page replacement | FIFO 15, LRU 12, Optimal 9 faults; Belady's anomaly series | yes / yes / yes / 5000 references (invariants, Optimal <= FIFO, LRU) |
| Segmentation | textbook table ((2,53)=4353, (3,852)=4052, (0,1222) trap) | yes / yes / yes / 3000 segments |
| Fragmentation | hand-placed First/Best/Worst Fit in both modes; internal vs external distinction | yes / yes / yes / 1000 processes x 400 blocks x 6 configurations |
| Disk | textbook queue: FCFS 640, SSTF 236, SCAN 331, C-SCAN 382, LOOK 299, C-LOOK 322 (+ down direction, no-jump) | yes / yes / yes / 3000 requests |
| Multiprogramming | hand-simulated example (completion 6/8/10, CPU 100%, memory 72%, degree 1.8) | yes / yes / yes / 500 jobs (no overlap, memory bounds) |
| GUI (fake tkinter) | every page: example runs, blank input, bad values, large random data | logic only |

## NOT verified in this environment (please do on Windows before submitting)
* The sandbox had **no Tkinter and no display**, so the real window was **never opened here**. `test_gui_smoke.py` runs
  our GUI code against a fake tkinter, which cannot catch layout, colour, scrolling or real-Tk API problems.
* Manual checklist: launch `python main.py`; open all 8 pages; press *Load example* + *Run* on each; try an empty Run;
  add 300 random rows; resize the window; scroll with the mouse wheel; build the `.exe` with `build_exe.bat`.
* The `.exe` build was not performed (needs Windows + PyInstaller).
