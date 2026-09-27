import secrets
import string


def generate_short_code(length=6):
    characters = string.ascii_letters + string.digits

    return ''.join(
        secrets.choice(characters)
        for _ in range(length)
    )
if __name__ == "__main__":
    print(generate_short_code())