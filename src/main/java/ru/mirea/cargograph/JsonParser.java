package ru.mirea.cargograph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON parser used for Cargo sparse-index records; no external library is needed. */
public final class JsonParser {
    public Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) throw parser.error("Лишние символы после JSON-значения");
        return value;
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
        }

        private Object readValue() {
            skipWhitespace();
            if (isAtEnd()) throw error("Неожиданный конец JSON");
            return switch (source.charAt(position)) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (take('}')) return result;
            do {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                result.put(key, readValue());
                skipWhitespace();
                if (take('}')) return result;
                expect(',');
            } while (true);
        }

        private List<Object> readArray() {
            List<Object> result = new ArrayList<>();
            position++;
            skipWhitespace();
            if (take(']')) return result;
            do {
                result.add(readValue());
                skipWhitespace();
                if (take(']')) return result;
                expect(',');
            } while (true);
        }

        private String readString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!isAtEnd()) {
                char current = source.charAt(position++);
                if (current == '"') return result.toString();
                if (current != '\\') { result.append(current); continue; }
                result.append(readEscape());
            }
            throw error("Незакрытая JSON-строка");
        }

        private char readEscape() {
            if (isAtEnd()) throw error("Незавершенная escape-последовательность");
            char escaped = source.charAt(position++);
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> readUnicode();
                default -> throw error("Неизвестная escape-последовательность");
            };
        }

        private char readUnicode() {
            if (position + 4 > source.length()) throw error("Неверный Unicode escape");
            String hex = source.substring(position, position + 4);
            position += 4;
            try { return (char) Integer.parseInt(hex, 16); }
            catch (NumberFormatException ex) { throw error("Неверный Unicode escape"); }
        }

        private Object readNumber() {
            int start = position;
            while (!isAtEnd() && "-+0123456789.eE".indexOf(
                    source.charAt(position)) >= 0) position++;
            if (start == position) throw error("Ожидалось JSON-значение");
            String number = source.substring(start, position);
            try { return Double.valueOf(number); }
            catch (NumberFormatException ex) { throw error("Некорректное число: " + number); }
        }

        private Object readLiteral(String literal, Object value) {
            if (!source.startsWith(literal, position)) throw error("Некорректный литерал");
            position += literal.length();
            return value;
        }

        private void expect(char expected) {
            if (!take(expected)) throw error("Ожидался символ '" + expected + "'");
        }

        private boolean take(char expected) {
            if (isAtEnd() || source.charAt(position) != expected) return false;
            position++;
            return true;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean isAtEnd() {
            return position >= source.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " (позиция " + position + ")");
        }
    }
}
