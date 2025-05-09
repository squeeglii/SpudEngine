import csv

# https://matplotlib.org/stable/gallery/lines_bars_and_markers/barchart.html <3
# matplotlib my beloved. This would've sucked in excel.
import matplotlib.pyplot as plt
import matplotlib.ticker as tickers
import numpy as np

from sys import argv

defaultPath = "data.csv"
device_colours = {
    "Laptop": "tab:blue",
    "Desktop": "tab:red"
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

    return processed_data

def plot_test_case(data: list[Entry], test_case: str):

    # Filter to ONLY this test case.
    relevant_data = [ ]
    device_types = [ ]
    renderer_types = [ ]

    for entry in data:
        if not entry.test_case.lower().startswith(test_case.lower()):
            continue

        relevant_data.append(entry)

        if entry.device not in device_types:
            device_types.append(entry.device)

        if entry.renderer not in renderer_types:
            renderer_types.append(entry.renderer)

    print("Filtered to: ", test_case)
    print("Device Types: ", device_types)
    print("Renderer Types: ", renderer_types)

    entry_count = len(relevant_data)
    device_count = len(device_types)
    renderer_count = len(renderer_types)

    if entry_count != device_count * renderer_count:
        raise InvalidDatasetError(f"Weirdly formatted dataset! devices ({device_count}) * renderers ({renderer_count}) != filtered data amount ({entry_count})")

    print("Plotting data...")
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

    ## 0-0.4 0.5 - 0.9

    print(x_label_positions)
    print(bar_positions)
    print(group_width)

    y = np.zeros(len(relevant_data))
    interval_min = np.zeros(len(relevant_data))
    interval_max = np.zeros(len(relevant_data))
    labels = []
    bar_colours = []

    for i, entry in enumerate(relevant_data):
        print("entry: ", entry.get_sample_min(), entry.get_sample_max(), entry.get_sample_avg())
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

    ax.set_xticks(x_label_positions)
    ax.set_xticklabels(x_labels)

    ax.bar(bar_positions, y, width=bar_width, label=labels, color=bar_colours, zorder=10)
    ax.errorbar(bar_positions, y, yerr=interval_range, fmt="x", color="#000000", capsize=6, capthick=1, zorder=20)

    ax.set_title(f"Test Case {test_case}")
    ax.legend(device_colours, title="Device Types")

    print("Plotted!")
    plt.show()

def main():
    run_config = parse_path_from_args()
    processed_data = process(run_config[0], run_config[1])

    plot_test_case(processed_data, "1")


if __name__ == "__main__":
    main()