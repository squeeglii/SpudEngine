import csv

# https://matplotlib.org/stable/gallery/lines_bars_and_markers/barchart.html <3
# matplotlib my beloved. This would've sucked in excel.
import matplotlib.pyplot as plt
import matplotlib.ticker as tickers
import numpy as np

from sys import argv

defaultPath = "data.csv"
device_colours = {
    "Laptop": "tab:purple",
    "Desktop": "tab:orange"
}
reformatted_titles = {
    "0": "Summary (w/o stress test)",
    "1": "Test Case #1: Typical Environment",
    "2": "Test Case #2: Tight Space",
    "3": "Test Case #3: Tight Recursive Space",
    "4": "Test Case #4: Background Recursion",
    "5": "Test Case #5: The Clean Room",
    "6": "Test Case #6: Stress Test"
}

class InvalidEntryError(Exception):

    def __init__(self, text):
        self.add_note(text)

class InvalidDatasetError(Exception):

    def __init__(self, text):
        self.add_note(text)

class Sample(object):

    def __init__(self, min_interval, max_interval, avg):
        self.min = int(min_interval)   # to 1ms
        self.max = int(max_interval)   # to 1ms
        self.avg = float(avg)          # to 0.001ms

class Entry(object):

    def __init__(self, test_case: str, device: str, renderer: str):
        self.test_case = test_case
        self.device = device
        self.renderer = renderer
        self.samples = [ ]

    def add_sample(self, entry):
        self.samples.append(entry)

    def get_sample_min(self):
        if len(self.samples) == 0:
            raise InvalidEntryError("Tried to sample from empty entry")

        val = self.samples[0].min

        for sample in self.samples[1:]:
            val = min(val, sample.min)

        return val

    def get_sample_max(self):
        if len(self.samples) == 0:
            raise InvalidEntryError("Tried to sample from empty entry")

        val = self.samples[0].max

        for sample in self.samples[1:]:
            val = max(val, sample.max)

        return val

    def get_sample_avg(self):
        if len(self.samples) == 0:
            raise InvalidEntryError("Tried to sample from empty entry")

        val = 0

        for sample in self.samples:
            val += sample.avg

        return val / len(self.samples)

    def __str__(self):
        return f"Entry ({len(self.samples)}x samples) - [ {self.test_case} | {self.device} | {self.renderer} ]"

def filter_only_starting_with(data: list[Entry], staring_with: str) -> list[Entry]:
    filtered_data = [ ]

    for entry in data:
        if not entry.test_case.lower().startswith(staring_with.lower()):
            continue

        filtered_data.append(entry)

    return filtered_data

def filter_remove_starting_with(data: list[Entry], staring_with: str) -> list[Entry]:
    filtered_data = [ ]

    for entry in data:
        if entry.test_case.lower().startswith(staring_with.lower()):
            continue

        filtered_data.append(entry)

    return filtered_data

def parse_path_from_args():
    if len(argv) < 3:
        print("Incomplete parameters (include [start row num] and [path]) - using defaults.")
        return defaultPath, 2

    return str.join(" ", argv[2:]),  int(argv[1])

def process(path, start_row):

    processed_data = []

    with open(path) as data:
        for i in range(start_row):
            data.readline() # skip first few lines.

        last_entry: Entry|None = None

        for i, line in enumerate(data):
            line_number = i + start_row
            split_line = line.split(",")
            minInterval = split_line[4]
            maxInterval = split_line[5]
            avg = split_line[7]
            sample_number = int(split_line[3])

            if minInterval == '' or maxInterval == '' or avg == '':
                print(f"Skipping entry [{line_number}] - blank data.")
                continue

            sample: Sample = Sample(minInterval, maxInterval, avg)

            if sample_number == 1:
                if last_entry is not None:
                    processed_data.append(last_entry)
                    print(f"Committed entry: {last_entry}")
                last_entry = Entry(split_line[0], split_line[1], split_line[2])

            last_entry.add_sample(sample)

    if last_entry is not None:
        processed_data.append(last_entry)

    return processed_data

def create_summary(processed_data: list[Entry]):
    device_types = [ ]
    renderer_types = [ ]

    for entry in processed_data:
        if entry.device not in device_types:
            device_types.append(entry.device)

        if entry.renderer not in renderer_types:
            renderer_types.append(entry.renderer)

    new_entries: dict[str, Entry] = { }

    for renderer in renderer_types:
        for device in device_types:
            new_id = renderer + "-" + device
            new_entries[new_id] = Entry("0 - Summary", device, renderer)

    for entry in processed_data:
        new_id = entry.renderer + "-" + entry.device

        for sample in entry.samples:
            new_entries[new_id].add_sample(sample)

    # Processed data needs to be sent batched in renderer groups.
    # with a consistent device order.

    # i.e,      laptop naive, desktop naive, laptop multipass, desktop multipass...
    return new_entries.values()

# The data that needs to go in is order specific.
def plot_test_case_frame_intervals(processed_data: list[Entry], test_case_filter: str):

    # Filter to ONLY this test case.
    relevant_data = filter_only_starting_with(processed_data, test_case_filter)

    device_types = [ ]
    renderer_types = [ ]

    for entry in relevant_data:
        if entry.device not in device_types:
            device_types.append(entry.device)

        if entry.renderer not in renderer_types:
            renderer_types.append(entry.renderer)

    print()
    print("Plotting data...")
    print("Filtered to: ", test_case_filter)
    print("Device Types: ", device_types)
    print("Renderer Types: ", renderer_types)

    entry_count = len(relevant_data)
    device_count = len(device_types)
    renderer_count = len(renderer_types)

    if entry_count != device_count * renderer_count:
        raise InvalidDatasetError(f"Weirdly formatted dataset! devices ({device_count}) * renderers ({renderer_count}) != filtered data amount ({entry_count})")

    fig, ax = plt.subplots()

    bar_width = 0.4
    bar_gap = 0.1

    group_width = ((bar_width + bar_gap) * len(device_types)) - bar_gap
    group_gap = 1

    x_label_positions = []
    x_labels = []
    bar_positions = [ ]

    for group_id, renderer in enumerate(renderer_types):
        group_start = group_id * (group_width + group_gap)
        x_label_positions.append(group_start + (group_width/2))
        x_labels.append(renderer + "\n Renderer")

        for barNum, device in enumerate(device_types):
            bar_positions.append(group_start + (0.5 * bar_width) + (barNum * (bar_width + bar_gap)))

    y = np.zeros(len(relevant_data))
    interval_min = np.zeros(len(relevant_data))
    interval_max = np.zeros(len(relevant_data))
    labels = []
    bar_colours = []

    for i, entry in enumerate(relevant_data):
        y[i] = entry.get_sample_avg()
        interval_min[i] = entry.get_sample_min()
        interval_max[i] = entry.get_sample_max()
        labels.append(entry.device)
        bar_colours.append(device_colours[entry.device])

    interval_range = [y - interval_min, interval_max - y]

    ax.yaxis.set_major_locator(tickers.MultipleLocator(2))
    ax.yaxis.set_minor_locator(tickers.MultipleLocator(0.1))
    ax.yaxis.grid(which='major', color="#cccccc", linewidth=1.0, zorder=0)
    ax.yaxis.grid(which='minor', color="#eeeeee", linewidth=0.5, zorder=0)
    ax.yaxis.minorticks_on()
    ax.set_ylabel("Frame Interval (ms)")

    ax.set_xticks(x_label_positions)
    ax.set_xticklabels(x_labels)

    ax.bar(bar_positions, y, width=bar_width, label=labels, color=bar_colours, zorder=10)
    ax.errorbar(bar_positions, y, yerr=interval_range, fmt="x", color="#000000", capsize=6, capthick=1, zorder=20)
    ax.legend(device_types, title="Device Types")

    fig.suptitle("Frame Timing (lower is better)")

    if test_case_filter in reformatted_titles:
        ax.set_title(reformatted_titles[test_case_filter], style='italic')
    else:
        ax.set_title(f"Test Case {test_case_filter}", style='italic')

    print("Plotted!")
    plt.show()

def plot_test_case_framerates(processed_data: list[Entry], test_case_filter: str):

    # Filter to ONLY this test case.
    relevant_data = filter_only_starting_with(processed_data, test_case_filter)
    device_types = [ ]
    renderer_types = [ ]

    for entry in relevant_data:
        if entry.device not in device_types:
            device_types.append(entry.device)

        if entry.renderer not in renderer_types:
            renderer_types.append(entry.renderer)

    print()
    print("Plotting data...")
    print("Filtered to: ", test_case_filter)
    print("Device Types: ", device_types)
    print("Renderer Types: ", renderer_types)

    entry_count = len(relevant_data)
    device_count = len(device_types)
    renderer_count = len(renderer_types)

    if entry_count != device_count * renderer_count:
        raise InvalidDatasetError(f"Weirdly formatted dataset! devices ({device_count}) * renderers ({renderer_count}) != filtered data amount ({entry_count})")

    fig, ax = plt.subplots()

    bar_width = 0.4
    bar_gap = 0.1

    group_width = ((bar_width + bar_gap) * len(device_types)) - bar_gap
    group_gap = 1

    x_label_positions = []
    x_labels = []
    bar_positions = [ ]

    for group_id, renderer in enumerate(renderer_types):
        group_start = group_id * (group_width + group_gap)
        x_label_positions.append(group_start + (group_width/2))
        x_labels.append(renderer + "\n Renderer")

        for barNum, device in enumerate(device_types):
            bar_positions.append(group_start + (0.5 * bar_width) + (barNum * (bar_width + bar_gap)))

    y = np.zeros(len(relevant_data))
    interval_min = np.zeros(len(relevant_data))
    interval_max = np.zeros(len(relevant_data))
    labels = []
    bar_colours = []

    for i, entry in enumerate(relevant_data):
        y[i] = 1000 / entry.get_sample_avg()
        interval_min[i] = entry.get_sample_min()
        interval_max[i] = entry.get_sample_max()
        labels.append(entry.device)
        bar_colours.append(device_colours[entry.device])

    interval_range = [y - interval_min, interval_max - y]

    ax.yaxis.set_major_locator(tickers.MultipleLocator(15))
    ax.yaxis.set_minor_locator(tickers.MultipleLocator(5))
    ax.yaxis.grid(which='major', color="#cccccc", linewidth=1.0, zorder=0)
    ax.yaxis.grid(which='minor', color="#eeeeee", linewidth=0.5, zorder=0)
    ax.yaxis.minorticks_on()
    ax.set_ylabel("Framerate (frames/s)")

    ax.set_xticks(x_label_positions)
    ax.set_xticklabels(x_labels)

    ax.bar(bar_positions, y, width=bar_width, label=labels, color=bar_colours, zorder=10)
    #ax.errorbar(bar_positions, y, yerr=interval_range, fmt="x", color="#000000", capsize=6, capthick=1, zorder=20)
    ax.legend(device_types, title="Device Types")

    fig.suptitle("Framerates (higher is better)")

    if test_case_filter in reformatted_titles:
        ax.set_title(reformatted_titles[test_case_filter], style='italic')
    else:
        ax.set_title(f"Test Case {test_case_filter}", style='italic')

    print("Plotted!")
    plt.show()

def main():
    run_config = parse_path_from_args()
    processed_data = process(run_config[0], run_config[1])

    plot_test_case_frame_intervals(processed_data, "1")
    plot_test_case_frame_intervals(processed_data, "2")
    plot_test_case_frame_intervals(processed_data, "3")
    plot_test_case_frame_intervals(processed_data, "4")
    plot_test_case_frame_intervals(processed_data, "5")
    plot_test_case_frame_intervals(processed_data, "6")

    data_without_stress = filter_remove_starting_with(processed_data, "6")
    summary = create_summary(data_without_stress)

    plot_test_case_frame_intervals(summary, "0")
    plot_test_case_framerates(summary, "0")


if __name__ == "__main__":
    main()