import random
import string


class ShortCodeGenerator:
    """Generates random alphanumeric short codes."""

    def __init__(self, length=6):
        self.length = length
        self.characters = string.ascii_letters + string.digits

    def generate(self):
        """Generate a random short code."""
        return ''.join(
            random.choices(self.characters, k=self.length)
        )