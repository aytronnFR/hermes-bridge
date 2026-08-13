import unittest

from app.audio_processing import ffmpeg_command


class AudioProcessingTest(unittest.TestCase):
    def test_applies_subtle_voice_eq_and_compression(self):
        command = ffmpeg_command()
        filter_graph = command[command.index("-af") + 1]

        self.assertIn("highpass=f=80", filter_graph)
        self.assertIn("equalizer=f=2800:t=q:w=1.2:g=2", filter_graph)
        self.assertIn("acompressor=threshold=-18dB:ratio=1.5:attack=20:release=180", filter_graph)


if __name__ == "__main__":
    unittest.main()
